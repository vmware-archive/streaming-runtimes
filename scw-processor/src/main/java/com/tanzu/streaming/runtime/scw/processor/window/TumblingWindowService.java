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

import com.tanzu.streaming.runtime.scw.processor.window.state.StateEntry;

import org.springframework.messaging.support.MessageBuilder;

public interface TumblingWindowService {

        /**
         * 
         * @param windowStartTime
         * @param windowEndTime
         * @param windowAggregate
         * @param isPartial
         * @return
         */
        List<MessageBuilder<?>> computeWindowAggregate(Duration windowStartTime, Duration windowEndTime,
                        StateEntry windowAggregate, boolean isPartial);

        /**
         * Send the collected aggregation downstream.
         * 
         * @param aggregateEventTime Event time to be used with the message sent.
         * @param outputWatermark Watermark to be propagated with the message sent.
         * @param messageBuilder Builder with the message aggregate to be sent.
         */
        void send(Duration aggregateEventTime, Duration outputWatermark, MessageBuilder<?> messageBuilder);

        void handleLateEvent(Duration aggregateEventTime, Duration outputWatermark,
                        MessageBuilder<?> messageBuilder);

        void releaseWindow(long windowStartTimeNs, boolean removeWindow, boolean isPartial, boolean isLateEventResend);

        long getOldestWindowId();
}
