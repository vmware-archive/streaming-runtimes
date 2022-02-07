package net.tzolov.poc.uppercasegrpc;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UppercaseGrpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(UppercaseGrpcApplication.class, args);
    }

    @Bean
    public Function<String, String> uppercase() {
        return String::toUpperCase;
    }
}
