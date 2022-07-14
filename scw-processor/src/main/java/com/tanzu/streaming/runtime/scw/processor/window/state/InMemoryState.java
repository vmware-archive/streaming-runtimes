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
package com.tanzu.streaming.runtime.scw.processor.window.state;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.CollectionUtils;

public class InMemoryState implements State, AutoCloseable {

    private static final Log logger = LogFactory.getLog(InMemoryState.class);

    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Message<byte[]>>> tumblingWindows = new ConcurrentHashMap<>();

    /**
     * @return Returns current window keys (e.g. timestamps). Order is not guaranteed.
     */
    @Override
    public Set<Long> keys() {
        return this.tumblingWindows.keySet();
    }

    @Override
    public void put(long timestamp, Map<String, Object> headers, byte[] payload) {
        if (!this.tumblingWindows.containsKey(timestamp)) {
            logger.info(">> START WINDOW: " + timestamp);
            this.tumblingWindows.putIfAbsent(timestamp, new ConcurrentLinkedQueue<>());
        }

        // Add the input message to the tumbling window's queue
        Optional.ofNullable(this.tumblingWindows.get(timestamp))
                .ifPresent(windowQueue -> windowQueue
                        .add(MessageBuilder.withPayload(payload).copyHeaders(headers).build()));
    }

    @Override
    public StateEntry get(long timestamp) {
        return toSateEntry(timestamp, this.tumblingWindows.get(timestamp));
    }

    @Override
    public StateEntry delete(long timestamp) {
        return toSateEntry(timestamp, this.tumblingWindows.remove(timestamp));
    }

    private StateEntry toSateEntry(long timestamp, ConcurrentLinkedQueue<Message<byte[]>> queue) {
        if (!CollectionUtils.isEmpty(queue)) {
            List<byte[]> payloads = queue.stream().map(m -> m.getPayload()).collect(Collectors.toList());
            MessageHeaders headers = queue.iterator().next().getHeaders();
            return new StateEntry(timestamp, headers, payloads);
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if (this.tumblingWindows != null) {
            this.tumblingWindows.clear();
        }
    }
}
