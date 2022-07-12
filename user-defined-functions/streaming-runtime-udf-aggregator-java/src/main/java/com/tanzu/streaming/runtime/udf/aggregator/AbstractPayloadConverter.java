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

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanzu.streaming.runtime.processor.common.avro.AvroSchemaReaderWriter;
import com.tanzu.streaming.runtime.processor.common.avro.AvroUtil;

import org.springframework.util.MimeType;

/**
 * PayloadConverter that can convert Avro and JSON (or Java Map Compatible) payload formats. For Avro to work you have
 * to provide the Avro Schema URI.
 */
public abstract class AbstractPayloadConverter<T> implements PayloadConverter<T> {

    private final String avroSchemaUri;
    private AvroSchemaReaderWriter avroReader;

    public AbstractPayloadConverter(String avroSchemaUri) {
        this.avroSchemaUri = avroSchemaUri;
    }

    protected Object fromPayloadRaw(byte[] payload, MimeType payloadContentType) {
        if (AvroUtil.isAvroContentType(payloadContentType)) {
            return this.getAvroSchemaReaderWriter().readRecord(payload);
        }
        else {
            try {
                return new ObjectMapper().readValue(payload, Map.class);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    protected AvroSchemaReaderWriter getAvroSchemaReaderWriter() {
        if (this.avroReader == null) {
            this.avroReader = AvroSchemaReaderWriter
                    .from(AvroSchemaReaderWriter.toResource(this.avroSchemaUri));
        }
        return this.avroReader;
    }
}
