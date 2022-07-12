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

import org.springframework.util.MimeType;

/**
 * Converts from bytes payloads to a record (e.g. avro or JSON map) and from record to byte array payload.
 */
public interface PayloadConverter<T> {
    /**
     * Convert byte array payload into a record.
     * @param payload byte array payload to convert
     * @param payloadContentType input payload content type.
     * @return Returns a
     */
    T fromPayload(byte[] payload, MimeType payloadContentType);

    /**
     * Covert from the domain type to wire byte array payload.
     * @param record domain type record.
     * @param targetContentType target content type.
     * @return Returns byte array encoded content in accordance with the target content type.
     */
    byte[] toPayload(T record, MimeType targetContentType);
}
