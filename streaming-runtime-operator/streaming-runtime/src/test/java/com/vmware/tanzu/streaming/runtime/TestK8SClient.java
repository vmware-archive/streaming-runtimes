package com.vmware.tanzu.streaming.runtime;

import java.io.IOException;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.ApiextensionsV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinition;
import org.yaml.snakeyaml.Yaml;

import org.springframework.core.io.DefaultResourceLoader;

import static org.awaitility.Awaitility.await;

public class TestK8SClient {
	private static final String CLUSTER_STREAM_CRD_URI = "file:/Users/ctzolov/Dev/projects/tanzu/streaming-runtime-parent/crds/cluster-stream-crd.yaml";
	private final AppsV1Api appsV1Api;
	private final ApiextensionsV1Api apiextensionsV1Api;
	private final CoreV1Api coreV1Api;
	private final V1CustomResourceDefinition clusterStreamCrdDefinition;

	public TestK8SClient(ApiClient apiClient) throws IOException {
		appsV1Api = new AppsV1Api(apiClient);
		apiextensionsV1Api = new ApiextensionsV1Api(apiClient);
		coreV1Api = new CoreV1Api(apiClient);
		Yaml yaml = new Yaml();
		clusterStreamCrdDefinition = yaml.loadAs(
				new DefaultResourceLoader().getResource(CLUSTER_STREAM_CRD_URI).getInputStream(),
				V1CustomResourceDefinition.class);
		;
	}

	public void createCrd() throws ApiException {
		apiextensionsV1Api.createCustomResourceDefinition(clusterStreamCrdDefinition, null, null, null);
		await("CLUSTER STREAM CRD")
				.until(() -> apiextensionsV1Api.readCustomResourceDefinition(
						clusterStreamCrdDefinition.getMetadata().getName(), null) != null);
	}
}
