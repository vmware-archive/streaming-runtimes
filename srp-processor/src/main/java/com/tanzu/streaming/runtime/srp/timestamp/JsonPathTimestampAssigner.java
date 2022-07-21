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
package com.tanzu.streaming.runtime.srp.timestamp;

import java.io.IOException;

import com.tanzu.streaming.runtime.processor.common.avro.AvroMessageReader;
import com.tanzu.streaming.runtime.processor.common.avro.AvroUtil;

import org.springframework.integration.json.JsonPathUtils;
import org.springframework.messaging.Message;

/**
 * Uses JsonPath expression to extract the timestamp form the the payload. Works with any payloads that can be converted
 * into JSON internally.
 * 
 * In case of Avro payload format, requires an avroMessageReader to parse it into GenericRecord. The avroMessageReader
 * is not needed for non Avro payload formats.
 */
public class JsonPathTimestampAssigner implements RecordTimestampAssigner<byte[]> {

    private final String jsonPath;

    private final AvroMessageReader avroMessageReader;

    public JsonPathTimestampAssigner(String jsonPath, AvroMessageReader avroMessageReader) {
        this.jsonPath = jsonPath.startsWith("$.") ? jsonPath : "$." + jsonPath;
        this.avroMessageReader = avroMessageReader;
    }

    @Override
    public long extractTimestamp(Message<byte[]> message) {
        try {
            Object payload = AvroUtil.toJsonPayload(message, avroMessageReader);
            Object result = JsonPathUtils.evaluate(payload, jsonPath);
            if (result != null && result instanceof Number) {
                return ((Number) result).longValue();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return RecordTimestampAssigner.NO_TIMESTAMP;
    }

    public static void main(String[] args) throws IOException {
        String json = "{" +
                "\"firstName\": \"John\"," +
                "\"lastName\": \"doe\"," +
                "\"age\": 26," +
                "\"address\": {" +
                "\"streetAddress\": \"naist street\"," +
                "\"city\": \"Nara\"," +
                "\"postalCode\": \"630-0192\"" +
                "}," +
                "\"phoneNumbers\": [" +
                "{" +
                "\"type\": \"iPhone\"," +
                "\"number\": \"0123-4567-8888\"" +
                "}," +
                "{" +
                "\"type\": \"home\"," +
                "\"number\": \"0123-4567-8910\"" +
                "}" +
                "]" +
                "}";

        Object value = JsonPathUtils.evaluate(json.getBytes(), "$.lastName");
        System.out.println(value);
    }
}
