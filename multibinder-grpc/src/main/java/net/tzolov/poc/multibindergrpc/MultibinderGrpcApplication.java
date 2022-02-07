package net.tzolov.poc.multibindergrpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.grpc.FunctionGrpcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.cloud.function.grpc.GrpcUtils;

import java.util.function.Function;

@SpringBootApplication
@EnableConfigurationProperties(FunctionGrpcProperties.class)
public class MultibinderGrpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultibinderGrpcApplication.class, args);
    }

    @Bean
    public Function<Message<byte[]>, Message<byte[]>> proxy(FunctionGrpcProperties properties) {
        return message -> GrpcUtils.requestReply("localhost", properties.getPort(), message);
    }
}
