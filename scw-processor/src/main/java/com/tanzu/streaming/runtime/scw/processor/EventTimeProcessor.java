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

import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;

import org.springframework.messaging.Message;

/**
 * Implemented by event-time aware processors. Implementations can be stateful (e.g. time-window aggregations) or
 * stateless (e.g. source watermark generators or mapping processors).
 */
public interface EventTimeProcessor {
    /**
     * Handles a new time-event message.
     * @param message
     */
    void onNewMessage(Message<byte[]> message);

    WatermarkService getWatermarkService();
}
