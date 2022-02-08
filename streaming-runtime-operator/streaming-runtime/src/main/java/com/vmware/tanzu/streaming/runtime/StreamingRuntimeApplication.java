package com.vmware.tanzu.streaming.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StreamingRuntimeProperties.class)
public class StreamingRuntimeApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamingRuntimeApplication.class, args);
	}

}
