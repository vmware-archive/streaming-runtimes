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
package com.tanzu.streaming.runtime.scw.timestamp;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Lookups the timestamp from pre-configured message's header.
 */
public class MessageHeaderTimestampAssigner implements RecordTimestampAssigner<byte[]> {

    private final String headerName;

    public MessageHeaderTimestampAssigner(String headerName) {
        Assert.hasText(headerName, "Header name can not be empty!");
        this.headerName = headerName;
    }

    @Override
    public long extractTimestamp(Message<byte[]> message) {
        if (!message.getHeaders().containsKey(this.headerName)) {
            return RecordTimestampAssigner.NO_TIMESTAMP;
        }
        return message.getHeaders().get(this.headerName, Long.class);
    }
}
