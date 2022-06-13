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
package com.tanzu.streaming.runtime.processor.common.avro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.util.MimeType;

public class AvroUtil {

    private static ContentTypeResolver CONTENT_TYPE_RESOLVER = new DefaultContentTypeResolver();

    public final static MimeType MIMETYPE_AVRO = MimeType.valueOf("application/*+avro");

    public static MimeType resolveContentType(Message<?> message) {
        return CONTENT_TYPE_RESOLVER.resolve(message.getHeaders());
    }

    public static boolean isAvroContentType(Message<?> message) {
        return MIMETYPE_AVRO.isCompatibleWith(resolveContentType(message));
    }

    public static boolean isAvroContentType(MimeType mime) {
        return MIMETYPE_AVRO.isCompatibleWith(mime);
    }

    public static byte[] toJson(GenericRecord record) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(), outputStream);
            DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(record.getSchema());
            writer.write(record, jsonEncoder);
            jsonEncoder.flush();
            return outputStream.toByteArray();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to convert to JSON.", e);
        }
    }

    public static Object toJsonPayload(Message<?> message, AvroMessageReader avroMessageReader) {
        Object payload = message.getPayload();
        if (AvroUtil.isAvroContentType(message)) {
            if (avroMessageReader == null) {
                throw new IllegalStateException("Received Avro message but no Avro Converter is configured!");
            }
            GenericRecord record = (payload instanceof GenericRecord) ? (GenericRecord) payload
                    : avroMessageReader.toGenericRecord(message);
            return AvroUtil.toJson(record); // covert to JSON
        }
        return payload;
    }
}
