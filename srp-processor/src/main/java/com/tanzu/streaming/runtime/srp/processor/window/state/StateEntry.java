/*
 * Copyright 2002-2020 the original author or authors.
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
package com.tanzu.streaming.runtime.srp.processor.window.state;

import java.util.Collection;
import java.util.Map;

public class StateEntry {

    private final long timestamp;
    private final Map<String, Object> headers;
    private final Collection<byte[]> payloads;

    public StateEntry(long timestamp, Map<String, Object> headers, Collection<byte[]> payloads) {
        this.timestamp = timestamp;
        this.headers = headers;
        this.payloads = payloads;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Collection<byte[]> getPayloads() {
        return payloads;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }
}
