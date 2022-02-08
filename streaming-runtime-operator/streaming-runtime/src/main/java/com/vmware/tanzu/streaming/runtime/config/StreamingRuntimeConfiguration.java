package com.vmware.tanzu.streaming.runtime.config;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.informer.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StreamingRuntimeConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(StreamingRuntimeConfiguration.class);

	@Bean(destroyMethod = "shutdown")
	ControllerManager controllerManager(SharedInformerFactory sharedInformerFactory, Controller[] controllers) {
		return new ControllerManager(sharedInformerFactory, controllers);
	}

	@Bean
	CommandLineRunner commandLineRunner(ControllerManager controllerManager) {
		return args -> {
			LOG.info("Start ControllerManager");
			try {
				new Thread(controllerManager, "ControllerManager").start();
			}
			catch (Exception exception) {
				LOG.info("Exit ControllerManager");
			}
		};
	}
}
