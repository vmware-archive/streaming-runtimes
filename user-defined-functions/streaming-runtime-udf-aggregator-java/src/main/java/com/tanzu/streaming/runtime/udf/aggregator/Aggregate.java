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
 * Primary hook to implement the user defined function (UDF). When receiving a new Time-Window aggregate of T records,
 * the Aggregator calls the aggregate method, sequentially, for every input T record. That way the aggregate
 * implementation can compute a new aggregate state to be sent downstream.
 */
public interface Aggregate<T> {
    /**
     * Method called for each element on the Time-Windowed aggregation.
     * @param headers - Headers for the (multipart) TWA input.
     * @param twaRecord Record from the aggregated TWA aggregation.
     * @param outputAggregatedState Computed output state. Every separate map entry would result in separate output
     * message.
     */
    void aggregate(MessageHeaders headers, T twaRecord, ConcurrentHashMap<String, T> outputAggregatedState);
}
