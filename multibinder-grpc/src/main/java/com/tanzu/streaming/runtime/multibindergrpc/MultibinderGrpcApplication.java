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
package com.tanzu.streaming.runtime.multibindergrpc;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.grpc.FunctionGrpcProperties;
import org.springframework.cloud.function.grpc.GrpcUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

@SpringBootApplication
@EnableConfigurationProperties(FunctionGrpcProperties.class)
public class MultibinderGrpcApplication {

    private static final String LOCALHOST = "localhost";

    public static void main(String[] args) {
        SpringApplication.run(MultibinderGrpcApplication.class, args);
    }

    @Bean
    public Function<Message<byte[]>, Message<byte[]>> proxy(FunctionGrpcProperties properties) {
        return message -> {            
            Message<byte[]> outMessage = GrpcUtils.requestReply(LOCALHOST, properties.getPort(), message);
            return outMessage;

            // return CloudEventMessageBuilder
            //         .withData(message.getPayload())
            //         .setSource(URI.create("https://spring.io/foos"))
            //         .setId(UUID.randomUUID().toString())
            //         .build();

        };
    }
}
