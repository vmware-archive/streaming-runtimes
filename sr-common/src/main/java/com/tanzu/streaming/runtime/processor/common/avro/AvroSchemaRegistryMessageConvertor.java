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

import java.util.Collections;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

public class AvroSchemaRegistryMessageConvertor extends AbstractMessageConverter implements AvroMessageReader {

    private final static MimeType AVRO_MIME_TYPE = new MimeType("application", "avro");

    private final Serde<GenericRecord> genericAvroSerde;

    public AvroSchemaRegistryMessageConvertor(String schemaRegistryUri) {
        super(AVRO_MIME_TYPE);
        this.genericAvroSerde = new GenericAvroSerde();
        this.genericAvroSerde.configure(
                Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUri),
                false);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    public GenericRecord toGenericRecord(Message<?>message) {
        if (message.getPayload() instanceof GenericRecord) {
            return (GenericRecord) message.getPayload();
        }
        return (GenericRecord) fromMessage(message, GenericRecord.class);
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
        if ((message.getPayload() instanceof byte[])) {
            return this.genericAvroSerde.deserializer().deserialize(null, (byte[]) message.getPayload());
        }
        return null;
    }

    @Override
    protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
        if ((payload instanceof GenericRecord)) {
            byte[] bytePayload = this.genericAvroSerde.serializer().serialize(null, (GenericRecord) payload);
            return MessageBuilder.withPayload(bytePayload).copyHeaders(headers).build();
        }
        return null;

    }

}
