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
package com.tanzu.streaming.runtime.srp.attic;

import java.util.stream.Collectors;

import com.tanzu.streaming.runtime.srp.processor.window.state.RocksDBWindowState;
import com.tanzu.streaming.runtime.srp.processor.window.state.StateEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocksDBTest {

    static final Logger logger = LoggerFactory.getLogger(RocksDBTest.class);

    private static final String dbPath = "./rocksdb-data/";
    private static final String cfdbPath = "./rocksdb-data-cf/";

    public static void main(String[] args) {
        try (RocksDBWindowState state = new RocksDBWindowState("./rocksdb-data/")) {
            state.initialize();
            // Message<byte[]> msg1 = MessageBuilder
            // .withPayload("test1".getBytes())
            // .setHeader("h1", "v1")
            // .build();
            // Message<byte[]> msg2 = MessageBuilder
            // .withPayload("tes2".getBytes())
            // .setHeader("h2", "v2")
            // .build();

            // state.put(666, msg1);
            // state.put(999, msg2);

            StateEntry s1 = state.get(666);
            System.out.println(">>> " + s1.getHeaders()
                    + s1.getPayloads().stream().map(p -> new String(p)).collect(Collectors.toList()));

            StateEntry s2 = state.get(999);
            System.out.println(">>> " + s2.getHeaders()
                    + s2.getPayloads().stream().map(p -> new String(p)).collect(Collectors.toList()));

            System.out.println(">>> KEYS: " + state.keys().toString());
            System.out.println(">>> END");

        }
    }

}
