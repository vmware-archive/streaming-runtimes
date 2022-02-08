package com.vmware.tanzu.streaming.runtime.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vmware.tanzu.streaming.apis.StreamingTanzuVmwareComV1alpha1Api;
import com.vmware.tanzu.streaming.runtime.EventRecorder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.EventsV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import okhttp3.Protocol;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ApiConfig {
	@Bean
	@Profile("!componenttests")
	ApiClient apiClient() throws IOException {
		ApiClient apiClient = ClientBuilder.defaultClient();
		apiClient.setHttpClient(apiClient.getHttpClient()
				.newBuilder()
				.protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
				.readTimeout(Duration.ZERO)
				.pingInterval(1, TimeUnit.MINUTES)
				.build());
		io.kubernetes.client.openapi.Configuration.setDefaultApiClient(apiClient);
		return apiClient;
	}

	@Bean
	SharedInformerFactory sharedInformerFactory(ApiClient apiClient) {
		return new SharedInformerFactory(apiClient);
	}

	@Bean
	CoreV1Api coreV1Api(ApiClient apiClient) {
		return new CoreV1Api(apiClient);
	}

	@Bean
	AppsV1Api appsV1Api(ApiClient client) {
		return new AppsV1Api(client);
	}

	@Bean
	EventRecorder eventClient(ApiClient client) {
		return new EventRecorder(new EventsV1Api(client));
	}

	@Bean
	StreamingTanzuVmwareComV1alpha1Api streamingTanzuVmwareComV1alpha1Api(ApiClient apiClient) {
		return new StreamingTanzuVmwareComV1alpha1Api(apiClient);
	}

	@Bean
	public Lister<V1ConfigMap> configMapLister(ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
		GenericKubernetesApi<V1ConfigMap, V1ConfigMapList> genericApi =
				new GenericKubernetesApi<>(V1ConfigMap.class, V1ConfigMapList.class,
						"", "v1", "configmaps", apiClient);
		SharedIndexInformer<V1ConfigMap> informer = sharedInformerFactory.sharedIndexInformerFor
				(genericApi, V1ConfigMap.class, 60 * 1000L);
		return new Lister<>(informer.getIndexer());
	}
	
	@Bean
	static ObjectMapper yamlMapper() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.registerModule(new JavaTimeModule());
		return mapper;
	}
}
