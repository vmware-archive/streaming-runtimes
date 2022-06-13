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

/**
 * Extracts the Event (or Processing) time to be assigned with the processed record. The timestamps can be extracted
 * from existing header or payload fields or computed from the message. The 'proc' is reserved and stands from
 * processing-timestamp.
 */
public interface RecordTimestampAssigner<T> {
    /**
     * The value that is passed to {@link #extractTimestamp} when there is no previous timestamp attached to the record.
     */
    long NO_TIMESTAMP = Long.MIN_VALUE;

    /**
     * Computes and assigns a timestamp for the record. Value is in milliseconds since the Epoch. This is independent of
     * any particular time zone or calendar.
     *
     * @param record The element that the timestamp will be assigned to.
     * @return The new timestamp.
     */
    long extractTimestamp(Message<T> record);
}
