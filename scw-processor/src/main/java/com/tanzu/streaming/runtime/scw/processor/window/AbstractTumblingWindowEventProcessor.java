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
package com.tanzu.streaming.runtime.scw.processor.window;

import java.time.Duration;
import java.util.List;

import com.tanzu.streaming.runtime.scw.processor.EventTimeProcessor;
import com.tanzu.streaming.runtime.scw.processor.window.state.State;
import com.tanzu.streaming.runtime.scw.processor.window.state.StateEntry;
import com.tanzu.streaming.runtime.scw.timestamp.RecordTimestampAssigner;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService.WatermarkUpdateStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public abstract class AbstractTumblingWindowEventProcessor implements EventTimeProcessor, TumblingWindowService {

    static final Log logger = LogFactory.getLog(AbstractTumblingWindowEventProcessor.class);

    public final static boolean IS_PARTIAL_RELEASE = true;

    public final static boolean REMOVE_WINDOW = true;

    private final State windowState;

    private final WatermarkService watermarkService;

    private Duration windowInterval = Duration.ofSeconds(3);

    private RecordTimestampAssigner<byte[]> timestampAssigner;

    private String id;

    public AbstractTumblingWindowEventProcessor(State windowState, WatermarkService watermarkService,
            Duration windowInterval, RecordTimestampAssigner<byte[]> timestampAssigner, String id) {
        this.windowState = windowState;
        this.watermarkService = watermarkService;
        this.windowInterval = windowInterval;
        this.timestampAssigner = timestampAssigner;
        this.id = id;
    }

    @Override
    public WatermarkService getWatermarkService() {
        return this.watermarkService;
    }

    @Override
    public void onNewMessage(Message<byte[]> inputMessage) {

        Duration messageTimestamp = Duration.ofMillis(this.timestampAssigner.extractTimestamp(inputMessage));

        if (this.watermarkService.updateWatermarks(inputMessage, messageTimestamp) == WatermarkUpdateStatus.DISCARDED) {
            logger.info("Discard message because it is behind the AllowedLateness");
            return;
        }

        long messageWindowStartTimeNs = this.computeWindowStartTime(
                messageTimestamp, this.windowInterval).toNanos();

        this.windowState.put(messageWindowStartTimeNs, inputMessage.getHeaders(), inputMessage.getPayload());

        // Check the existing windows for completion.
        this.evaluateWindowsCompletion(messageWindowStartTimeNs);
    }

    /**
     * For the message timestamp and windowInterval
     * 
     * @param messageTimestamp
     * @param windowInterval
     * @return
     */
    private Duration computeWindowStartTime(Duration messageTimestamp, Duration windowInterval) {
        return Duration.ofNanos(messageTimestamp.toNanos() - (messageTimestamp.toNanos() % windowInterval.toNanos()));
    }

    private void evaluateWindowsCompletion(long messageWindowStartTimeNs) {

        for (long windowStartTimeNs : this.windowState.keys()) {

            Duration windowEndTime = Duration.ofNanos(windowStartTimeNs + windowInterval.toNanos());

            if (this.watermarkService.isOlderThanWatermark(windowEndTime)) {

                if (!this.watermarkService.isAllowedLatenessEnabled()
                        || !this.watermarkService.isOlderThanAllowedLateness(windowEndTime)) {
                    this.releaseWindow(windowStartTimeNs, REMOVE_WINDOW, !IS_PARTIAL_RELEASE, false);
                }
                else {
                    if (messageWindowStartTimeNs == windowStartTimeNs) {
                        // re-send AllowedLateness window only if new message was added to it.
                        this.releaseWindow(windowStartTimeNs, !REMOVE_WINDOW, !IS_PARTIAL_RELEASE, true);
                    }
                }
            }
        }
    }

    @Override
    public void releaseWindow(long windowStartTimeNs, boolean removeWindow, boolean isPartial,
            boolean isLateEventResend) {

        logger.info(">> KEYS#: " + this.windowState.keys().size());

        StateEntry releasedAggregate = removeWindow
                ? this.windowState.delete(windowStartTimeNs)
                : this.windowState.get(windowStartTimeNs);

        if (releasedAggregate != null) {
            logger.info(id + ">> RELEASE window: " + windowStartTimeNs + ", payload count: "
                    + releasedAggregate.getPayloads().size());
            Duration outputEventTime = Duration.ofNanos(windowStartTimeNs + windowInterval.toNanos());
            Duration outputWatermark = Duration.ofMillis(this.watermarkService.computeWatermarkMs());

            Duration windowStartTime = Duration.ofNanos(windowStartTimeNs);
            Duration windowEndTime = windowStartTime.plus(windowInterval);

            List<MessageBuilder<?>> outputMessageBuilders = this.computeWindowAggregate(windowStartTime,
                    windowEndTime, releasedAggregate, isPartial);

            for (MessageBuilder<?> outputMessageBuilder : outputMessageBuilders) {

                outputMessageBuilder
                        .setHeader("eventtime", outputEventTime.toMillis())
                        .setHeader("watermark", outputWatermark.toMillis());

                if (isLateEventResend) {
                    this.handleLateEvent(outputEventTime, outputWatermark, outputMessageBuilder);
                }
                else {
                    this.send(outputEventTime, outputWatermark, outputMessageBuilder);
                }
            }
        }
        else {
            logger.info(id + "     -> Already Released: " + windowStartTimeNs);
            // Do nothing
        }
    }

    @Override
    public long getOldestWindowId() {
        return this.windowState.keys().stream()
                .mapToLong(l -> l)
                .min().orElse(-1L);
    }
}
