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

import java.util.Arrays;
import java.util.List;

import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollectionSeDe;
import com.tanzu.streaming.runtime.scw.ScwProcessorApplicationProperties.LateEventMode;
import com.tanzu.streaming.runtime.scw.processor.stateless.AbstractStatelessEventTimeProcessor;
import com.tanzu.streaming.runtime.scw.timestamp.RecordTimestampAssigner;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.function.grpc.GrpcUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class StatelessEventTimeProcessor extends AbstractStatelessEventTimeProcessor {

    static final Log logger = LogFactory.getLog(StatelessEventTimeProcessor.class);

    public static final String LOCALHOST = "localhost";

    private final ScwHeaderAugmenter headerAugmenter;
    private final ScwProcessorApplicationProperties properties;
    private final int grpcPort;
    private final StreamBridge streamBridge;

    public StatelessEventTimeProcessor(WatermarkService watermarkService,
            RecordTimestampAssigner<byte[]> timestampAssigner,
            ScwHeaderAugmenter headerAugmenter, ScwProcessorApplicationProperties properties,
            int grpcPort, StreamBridge streamBridge) {

        super(watermarkService, timestampAssigner);

        this.headerAugmenter = headerAugmenter;
        this.properties = properties;
        this.grpcPort = grpcPort;
        this.streamBridge = streamBridge;
    }

    @Override
    public void onNewMessage(Message<byte[]> message) {

        Response watermarkResponse = this.doOnNewMessage(message);

        MessageBuilder<?> messageBuilder = null;

        if (this.properties.isSkipUdf()) {
            // Object payload = AvroUtil.isAvroContentType(message) ? avroMessageReader.toGenericRecord(message)
            // : message.getPayload();
            messageBuilder = MessageBuilder
                    .withPayload(message.getPayload())
                    .copyHeaders(message.getHeaders());
        }
        else {
            if (this.properties.isForceGrpcPayloadCollection()) {
                MessageBuilder<byte[]> grpcPayloadCollectionMessageBuilder = GrpcPayloadCollectionSeDe
                        .encodeToGrpcPayloadCollection(Arrays.asList(message));

                Message<byte[]> grpcMessage = grpcPayloadCollectionMessageBuilder
                        // .copyHeaders(message.getHeaders())
                        .build();
                Message<byte[]> grpcResponseMessage = GrpcUtils.requestReply(LOCALHOST, this.grpcPort,
                        grpcMessage);

                List<MessageBuilder<?>> builders = GrpcPayloadCollectionSeDe
                        .decodeFromToGrpcPayloadCollection(grpcResponseMessage);
                messageBuilder = builders.get(0);
            }
            else {
                Message<byte[]> grpcResponseMessage = GrpcUtils.requestReply(LOCALHOST, this.grpcPort,
                        message);
                messageBuilder = MessageBuilder
                        .withPayload(grpcResponseMessage.getPayload())
                        .copyHeaders(grpcResponseMessage.getHeaders());
            }
        }

        // Make sure that the output message has an accurate watermark and eventtime headers set!
        messageBuilder
                .setHeader(WatermarkService.EVENTTIME_HEADER, watermarkResponse.getMessageEventTime().toMillis())
                .setHeader(WatermarkService.WATERMARK_HEADER, watermarkResponse.getWatermarkMs());

        Message<?> outputMessage = messageBuilder.build();

        // Add any pre-configured headers
        outputMessage = this.headerAugmenter.augment(outputMessage);

        switch (watermarkResponse.getMessageUpdateStatus()) {
        case VALID: {
            this.streamBridge.send(this.properties.getOutput().getDestination(), outputMessage);
            break;
        }
        case LATE: {
            if (this.properties.getLateEventMode() == LateEventMode.UPSERT) {
                this.streamBridge.send(this.properties.getOutput().getDestination(), outputMessage);
            }
            else if (this.properties.getLateEventMode() == LateEventMode.SIDE_CHANNEL) {
                this.streamBridge.send(this.properties.getOutput().getLateEventDestination(), outputMessage);
            }
            else if (this.properties.getLateEventMode() == LateEventMode.DROP) {
                // do noting
                logger.warn("DROP late massage:" + outputMessage);
            }
            break;
        }
        case DISCARDED: {
            // do nothing
            logger.warn("Discard late massage:" + outputMessage);
            break;
        }
        }
    }
}
