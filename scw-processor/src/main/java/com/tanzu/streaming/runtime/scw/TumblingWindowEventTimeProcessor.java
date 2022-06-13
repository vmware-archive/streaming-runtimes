/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tanzu.streaming.runtime.scw;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollectionSeDe;
import com.tanzu.streaming.runtime.scw.ScwProcessorApplicationProperties.LateEventMode;
import com.tanzu.streaming.runtime.scw.processor.window.AbstractTumblingWindowEventTimeProcessor;
import com.tanzu.streaming.runtime.scw.timestamp.RecordTimestampAssigner;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.function.grpc.GrpcUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class TumblingWindowEventTimeProcessor extends AbstractTumblingWindowEventTimeProcessor {

    private static final Log logger = LogFactory.getLog(TumblingWindowEventTimeProcessor.class);

    private static final String LOCALHOST = "localhost";

    private final StreamBridge streamBridge;

    private final int grpcPort;

    private ScwHeaderAugmenter outputHeadersAugmenter;

    private ScwProcessorApplicationProperties properties;

    public TumblingWindowEventTimeProcessor(ScwProcessorApplicationProperties properties,
            RecordTimestampAssigner<byte[]> timestampAssigner,
            WatermarkService watermarkService, StreamBridge streamBridge, int grpcPort,
            ScwHeaderAugmenter outputHeadersAugmenter) {

        super(properties.getName(), timestampAssigner, watermarkService, properties.getWindow(),
                properties.getIdleWindowTimeout());

        this.streamBridge = streamBridge;
        this.grpcPort = grpcPort;
        this.outputHeadersAugmenter = outputHeadersAugmenter;
        this.properties = properties;
    }

    @Override
    public List<MessageBuilder<?>> computeWindowAggregate(Duration windowStartTime, Duration windowEndTime,
            ConcurrentLinkedQueue<Message<byte[]>> windowAggregate, boolean isPartial) {

        MessageBuilder<byte[]> aggregatedGrpcMessageBuilder = GrpcPayloadCollectionSeDe
                .encodeToGrpcPayloadCollection(windowAggregate);

        aggregatedGrpcMessageBuilder
                .setHeader("windowStartTime", windowStartTime.toMillis())
                .setHeader("windowEndTime", windowEndTime.toMillis())
                .setHeader("partial", isPartial);

        if (this.properties.isSkipUdf()) {
            return Arrays.asList(aggregatedGrpcMessageBuilder);
        }

        // TODO: To log warning if the forceGrpcPayloadCollection=false is set.
        // When time-window aggregation is enabled the forceGrpcPayloadCollection is igntored.

        Message<byte[]> aggregatedGrpcMessage = aggregatedGrpcMessageBuilder.build();
        logger.info("aggregatedGrpcMessage Headers:" + aggregatedGrpcMessage.getHeaders().toString());
        Message<byte[]> grpcOutMessage = GrpcUtils.requestReply(LOCALHOST, this.grpcPort, aggregatedGrpcMessage);

        List<MessageBuilder<?>> builders = GrpcPayloadCollectionSeDe.decodeFromToGrpcPayloadCollection(grpcOutMessage);
        for (MessageBuilder<?> builder : builders) {
            builder.setHeader("windowStartTime", windowStartTime.toMillis())
                    .setHeader("windowEndTime", windowEndTime.toMillis())
                    .setHeader("partial", isPartial);
        }
        return builders;

    }

    @Override
    public void send(Duration aggregateEventTime, Duration outputWatermark, MessageBuilder<?> messageBuilder) {
        this.internalSend(this.properties.getOutput().getDestination(), aggregateEventTime, outputWatermark,
                messageBuilder);
    }

    @Override
    public void handleLateEvent(Duration aggregateEventTime, Duration outputWatermark,
            MessageBuilder<?> messageBuilder) {
        if (this.properties.getLateEventMode() == LateEventMode.UPSERT) {
            this.internalSend(this.properties.getOutput().getDestination(), aggregateEventTime, outputWatermark,
                    messageBuilder);
        }
        else if (this.properties.getLateEventMode() == LateEventMode.SIDE_CHANNEL) {
            this.internalSend(this.properties.getOutput().getLateEventDestination(), aggregateEventTime,
                    outputWatermark, messageBuilder);
        }
        else {
            logger.warn(String.format("Drop late event at: %s, watermark: %s, message: %s", aggregateEventTime,
                    outputWatermark, messageBuilder.build()));
        }
    }

    private void internalSend(String bindingName, Duration aggregateEventTime, Duration outputWatermark,
            MessageBuilder<?> messageBuilder) {
        Message<?> outputMessage = this.outputHeadersAugmenter.augment(messageBuilder.build());
        this.streamBridge.send(bindingName, outputMessage);
    }

}
