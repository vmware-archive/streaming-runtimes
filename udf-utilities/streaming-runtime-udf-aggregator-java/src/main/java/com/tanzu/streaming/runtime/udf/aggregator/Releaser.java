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
package com.tanzu.streaming.runtime.udf.aggregator;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.MessageHeaders;

/**
 * A call back interface that allows the UDF to refine the computed aggregate and target headers before they are sent.
 * For example it can remove (or add new) entries to the aggregate. Every aggregate entry results in a separate message
 * sent downstream!
 */
public interface Releaser<T> {

    /**
     * Callback invoked with the aggregated state before later is sent down stream.
     * @param outputAggregatedState aggregated state to sent.
     * @return Returns the refined aggregated state to sent.
     */
    ConcurrentHashMap<String, T> release(ConcurrentHashMap<String, T> outputAggregatedState);

    /**
     * Callback to augment the target headers to be used for sending the aggregate entries downstream. Same headers are
     * used for all messages part of this aggregate.
     * @param outputHeaders The message headers to augment;
     * @return Returns the augmented target headers.
     */
    MessageHeaders headers(MessageHeaders outputHeaders);
}
