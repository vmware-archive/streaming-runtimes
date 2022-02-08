package com.vmware.tanzu.streaming.runtime;

import java.io.FileReader;
import java.io.IOException;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("componenttests")
public class TestApiClientConfig {

	@Value("${clusterName}")
	private String clusterName;

	@Bean
	public ApiClient apiClient() {
		try {
			LocalClusterManager.createAndWait(clusterName);
			return ClientBuilder
					.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(clusterName)))
					.build()
					.setReadTimeout(0);
		}
		catch (IOException ioExc) {
			throw new ExtensionConfigurationException(
					"Failed to create ApiClient for '" + clusterName + "' cluster",
					ioExc);
		}
	}

}