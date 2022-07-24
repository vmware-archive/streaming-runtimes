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

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ByteString;
import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollection;
import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollection.Builder;
import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollectionSeDe;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

public class Aggregator<T> {

    private final Aggregate<T> aggregate;
    private final Releaser<T> releaser;
    private final PayloadConverter<T> payloadConverter;

    public Aggregator(Aggregate<T> aggregate, PayloadConverter<T> payloadConverter) {
        this(aggregate, new IdentityReleaser<>(), payloadConverter);
    }

    public Aggregator(Aggregate<T> aggregate, Releaser<T> releaser, PayloadConverter<T> payloadConverter) {
        this.aggregate = aggregate;
        this.releaser = releaser;
        this.payloadConverter = payloadConverter;
    }

    public Message<byte[]> onMessage(Message<byte[]> multipartMessage) {

        MimeType inputContentType = GrpcPayloadCollectionSeDe.CONTENT_TYPE_RESOLVER
                .resolve(multipartMessage.getHeaders());

        if (inputContentType == null) {
            // TODO
            System.out.println("Missing content type:" + inputContentType);
        }

        if (!inputContentType.getType().equals("multipart")) {
            System.out.println("Invalid content type:" + inputContentType);
        }

        MimeType payloadContentType = MimeType.valueOf("application/" + inputContentType.getSubtype());

        ConcurrentHashMap<String, T> resultAggregatedState = new ConcurrentHashMap<>();

        try {
            // Decode from PB array
            GrpcPayloadCollection payloadCollection = GrpcPayloadCollection.parseFrom(multipartMessage.getPayload());
            for (ByteString bs : payloadCollection.getPayloadList()) {
                T record = this.payloadConverter.fromPayload(bs.toByteArray(), payloadContentType);
                // Call UDF aggregate for every record in the TWA.
                this.aggregate.aggregate(multipartMessage.getHeaders(), record, resultAggregatedState);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        ConcurrentHashMap<String, T> finalResults = this.releaser.release(resultAggregatedState);

        MessageHeaders outputHeaders = new MessageHeaders(Collections.singletonMap(MessageHeaders.CONTENT_TYPE,
                MimeType.valueOf("multipart/" + inputContentType.getSubtype())));

        outputHeaders = this.releaser.headers(outputHeaders);

        // Encode to PB array
        Builder builder = GrpcPayloadCollection.newBuilder();
        for (String key : finalResults.keySet()) {
            T record = finalResults.get(key);
            builder.addPayload(ByteString.copyFrom(this.payloadConverter.toPayload(record, null)));
        }
        byte[] responseCollectionPayload = builder.build().toByteArray();

        return MessageBuilder.withPayload(responseCollectionPayload)
                .copyHeaders(outputHeaders)
                .build();
    }
}
