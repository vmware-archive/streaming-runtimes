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
package com.tanzu.streaming.runtime.processor.common.proto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollection.Builder;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

public class GrpcPayloadCollectionSeDe {

    public static ContentTypeResolver CONTENT_TYPE_RESOLVER = new DefaultContentTypeResolver();

    public static MessageBuilder<byte[]> encodeToGrpcPayloadCollection(Collection<Message<byte[]>> messages) {
        MimeType messageMimeType = null;
        Builder grpcBuilder = GrpcPayloadCollection.newBuilder();
        for (Message<byte[]> message : messages) {
            if (messageMimeType == null) {
                // Assumes that all messages are form the same MIME type!
                messageMimeType = CONTENT_TYPE_RESOLVER.resolve(message.getHeaders());
            }
            grpcBuilder.addPayload(ByteString.copyFrom(message.getPayload()));
        }
        if (messageMimeType == null) {
            throw new RuntimeException("Couldn't not resolve the MIME type for the aggregated messages!");
        }
        MessageBuilder<byte[]> messageBuilder = MessageBuilder
                .withPayload(grpcBuilder.build().toByteArray())
                .setHeader(MessageHeaders.CONTENT_TYPE, "multipart/" + messageMimeType.getSubtype());
        return messageBuilder;
    }

    public static List<MessageBuilder<?>> decodeFromToGrpcPayloadCollection(Message<byte[]> message) {
        try {
            MimeType decodedMimeType = null;
            MimeType grpcResponseMimeType = CONTENT_TYPE_RESOLVER.resolve(message.getHeaders());
            if (grpcResponseMimeType != null && grpcResponseMimeType.getType().equals("multipart")) {
                decodedMimeType = MimeType.valueOf("application/" + grpcResponseMimeType.getSubtype());
            }
            else {
                decodedMimeType = grpcResponseMimeType;
            }
            GrpcPayloadCollection payloadCollection = GrpcPayloadCollection.parseFrom(message.getPayload());
            List<MessageBuilder<?>> builders = new ArrayList<>(payloadCollection.getPayloadCount());
            for (ByteString bs : payloadCollection.getPayloadList()) {
                byte[] singlePayload = bs.toByteArray();
                MessageBuilder<?> messageBuilder = MessageBuilder
                        .withPayload(singlePayload)
                        .copyHeaders(message.getHeaders());
                if (decodedMimeType != null) {
                    messageBuilder.setHeader(MessageHeaders.CONTENT_TYPE, decodedMimeType);
                }

                builders.add(messageBuilder);
            }
            return builders;
        }
        catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Failed to decode grpc response to array of byte[]", e);
        }
    }

}
