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
package com.tanzu.streaming.runtime.scw.processor;

import java.time.Duration;

import com.tanzu.streaming.runtime.scw.timestamp.RecordTimestampAssigner;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService.WatermarkUpdateStatus;

import org.springframework.messaging.Message;

public abstract class AbstractStatelessEventTimeProcessor implements EventTimeProcessor {

    private final WatermarkService watermarkService;
    private final RecordTimestampAssigner<byte[]> timestampAssigner;

    public AbstractStatelessEventTimeProcessor(WatermarkService watermarkService,
            RecordTimestampAssigner<byte[]> timestampAssigner) {
        this.timestampAssigner = timestampAssigner;
        this.watermarkService = watermarkService;
    }

    @Override
    public WatermarkService getWatermarkService() {
        return this.watermarkService;
    }

    public Response doOnNewMessage(Message<byte[]> message) {
        Duration messageTimestamp = Duration.ofMillis(this.timestampAssigner.extractTimestamp(message));
        WatermarkUpdateStatus s = this.watermarkService.updateWatermarks(message, messageTimestamp);
        return new Response(s, messageTimestamp, this.watermarkService.computeWatermarkMs());
    }

    public static class Response {
        private final WatermarkUpdateStatus messageUpdateStatus;
        private final Duration messageEventTime;
        private final long watermarkMs;

        public Response(WatermarkUpdateStatus messageUpdateStatus, Duration messageEventTime, long watermarkMs) {
            this.messageUpdateStatus = messageUpdateStatus;
            this.messageEventTime = messageEventTime;
            this.watermarkMs = watermarkMs;
        }

        public Duration getMessageEventTime() {
            return messageEventTime;
        }

        public long getWatermarkMs() {
            return watermarkMs;
        }

        public WatermarkUpdateStatus getMessageUpdateStatus() {
            return messageUpdateStatus;
        }
    }
}
