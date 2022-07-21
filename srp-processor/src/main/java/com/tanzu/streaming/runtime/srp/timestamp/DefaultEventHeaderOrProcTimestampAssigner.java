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
package com.tanzu.streaming.runtime.srp.timestamp;

import org.springframework.messaging.Message;

/**
 * If the massage contains an 'eventtime' header, delegate to the MessageHeaderTimestampAssigner otherwise fallback to
 * the ProcTimestampAssigner.
 */
public class DefaultEventHeaderOrProcTimestampAssigner implements RecordTimestampAssigner<byte[]> {

    private static final String EVENTTIME_HEADER = "eventtime";

    private ProcTimestampAssigner procTimestampAssigner = new ProcTimestampAssigner();

    private MessageHeaderTimestampAssigner messageHeaderTimestampAssigner = new MessageHeaderTimestampAssigner(
            EVENTTIME_HEADER);

    @Override
    public long extractTimestamp(Message<byte[]> record) {
        if (record.getHeaders().containsKey(EVENTTIME_HEADER)) {
            return this.messageHeaderTimestampAssigner.extractTimestamp(record);
        }
        return this.procTimestampAssigner.extractTimestamp(record);
    }
}
