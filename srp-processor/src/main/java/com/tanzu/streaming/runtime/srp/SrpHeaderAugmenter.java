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
package com.tanzu.streaming.runtime.srp;

import java.util.Map;

import com.tanzu.streaming.runtime.processor.common.avro.AvroMessageReader;
import com.tanzu.streaming.runtime.processor.common.avro.AvroUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.json.JsonPathUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Use pre-configured map of (Header Name) -> (Payload Field Expression) to augment existing message with additional
 * headers computed from the input message payload.
 */
public class SrpHeaderAugmenter {

    private static final String PAYLOAD_PREFIX = "payload.";

    private static final String HEADER_PREFIX = "header.";

    private static final Log logger = LogFactory.getLog(SrpHeaderAugmenter.class);

    /**
     * Header Name to JsonPath expressions map.
     */
    private final Map<String, String> outputHeaders;

    /**
     * helper to convert avro payloads into json so the jsonPath can be applied.
     */
    private final AvroMessageReader avroMessageReader;

    public SrpHeaderAugmenter(Map<String, String> outputHeaders, AvroMessageReader avroMessageReader) {
        this.outputHeaders = outputHeaders;
        this.avroMessageReader = avroMessageReader;
    }

    /**
     * For input message and list of predefined headerName->jsonPath(payload) expressions, extracts the payload fields
     * into message headers and return new, augmented Message.
     * 
     * @param message input message to add new headers to.
     * @return Return new message that contains the new headers with values extracted from the input message payload.
     */
    public Message<?> augment(Message<?> message) {

        if (CollectionUtils.isEmpty(this.outputHeaders)) {
            // Nothing to augment!
            return message;
        }

        MessageBuilder<?> messageBuilder = MessageBuilder.fromMessage(message);

        for (String headerName : this.outputHeaders.keySet()) {

            String valueExpression = this.outputHeaders.get(headerName);

            if (!StringUtils.hasText(valueExpression)) {
                logger.error("Empty value expression for header name:" + headerName);
                break;
            }

            Object result = null;
            if (valueExpression.startsWith(HEADER_PREFIX)) {
                valueExpression = valueExpression.substring(HEADER_PREFIX.length());
                result = message.getHeaders().get(valueExpression);
            }
            else {
                String jsonPath = valueExpression.startsWith(PAYLOAD_PREFIX)
                        ? valueExpression.substring(PAYLOAD_PREFIX.length())
                        : valueExpression;
                Object jsonPayload = AvroUtil.toJsonPayload(message, avroMessageReader);
                try {
                    result = JsonPathUtils.evaluate(jsonPayload, jsonPath);
                }
                catch (Exception e) {
                    logger.error("Failed to run jsonPath: " + jsonPath + " on payload: " + jsonPayload, e);
                }
            }
            if (result != null) {
                logger.info(">>>> " + headerName + ": " + result);
                messageBuilder.setHeader(headerName, result);
            }
            else {
                logger.error("Failed to extract header value for: " + headerName);
            }
        }
        return messageBuilder.build();

    }

}
