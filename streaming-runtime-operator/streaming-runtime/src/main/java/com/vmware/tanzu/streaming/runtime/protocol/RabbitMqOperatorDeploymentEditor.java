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

package com.vmware.tanzu.streaming.runtime.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStream;
import com.vmware.tanzu.streaming.runtime.dataschema.AvroHelper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.CreateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Alternative Rabbit Protocol Deployment Editor that uses the Rabbit K8s
 * Operator internally.
 * (Experimental)
 */
@Component
public class RabbitMqOperatorDeploymentEditor implements ProtocolDeploymentAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(RabbitMqOperatorDeploymentEditor.class);

	private final ApiClient apiClient;
	private final CoreV1Api coreV1Api;
	private final DynamicKubernetesApi rmqClusterOperatorApi;

	private DynamicKubernetesApi rmqQueueOperatorApi;

	private DynamicKubernetesApi rmqExchangeOperatorApi;

	private DynamicKubernetesApi rmqBindingOperatorApi;

	private static final Resource rabbitmqService = toResource(
			"classpath:manifests/protocol/rabbitmq-op/RABBITMQ-CLUSTERSTREAM-AUTOPROVISION-TEMPLATE.yaml");

	public RabbitMqOperatorDeploymentEditor(ApiClient apiClient, CoreV1Api coreV1Api) {
		this.apiClient = apiClient;
		this.coreV1Api = coreV1Api;
		this.rmqClusterOperatorApi = new DynamicKubernetesApi(
				"rabbitmq.com",
				"v1beta1",
				"rabbitmqclusters", apiClient);

		this.rmqQueueOperatorApi = new DynamicKubernetesApi(
				"rabbitmq.com",
				"v1beta1",
				"queues", apiClient);

		this.rmqExchangeOperatorApi = new DynamicKubernetesApi(
				"rabbitmq.com",
				"v1beta1",
				"exchanges", apiClient);

		this.rmqBindingOperatorApi = new DynamicKubernetesApi(
				"rabbitmq.com",
				"v1beta1",
				"bindings", apiClient);

	}

	@Override
	public String getProtocolName() {
		return "rabbitmq";
	}

	@Override
	public String getProtocolDeploymentEditorName() {
		return "rabbitmq-operator";
	}

	@Override
	public void createIfNotFound(V1OwnerReference ownerReference, String namespace, V1alpha1ClusterStream clusterStream) throws ApiException {

		// Check if the RMQ pods are running
		if (isRunning(ownerReference, namespace)) { // TODO : check the Queue Exchange and Bindings existence
			LOG.info("RabbitMQ Cluster Instance {}/{} already running!", namespace, ownerReference.getName());
			return;
		}

		// Check if the RMQ Cluster Instance is already created
		KubernetesApiResponse<DynamicKubernetesObject> existingRmqClusterInstance = rmqClusterOperatorApi.get(namespace,
				ownerReference.getName());
		if (existingRmqClusterInstance.isSuccess() && existingRmqClusterInstance.getObject() != null) {
			LOG.info("RabbitMQ Cluster Instance {}/{} already created!", namespace, ownerReference.getName());
			return;
		}

		// Check if the RabbitMQ Cluster Operator is deployed
		if (!isPodExists("rabbitmq-cluster-operator", "rabbitmq-system", "rabbitmq-operator")) {
			// Install RMQ Operator
			// kubectl apply -f "https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml"
			LOG.warn("Missing RabbitMQ Cluster Operator. Later can be pre-installed like this:\n" +
					"kubectl apply -f \"https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml\"");
			throw new ApiException("Install the RabbitMQ Cluster Operator first!");
		}

		// Check if the RabbitMQ Cluster Operator is deployed
		if (!isPodExists("messaging-topology-operator", "rabbitmq-system", "rabbitmq-operator")) {
			// Install Messaging Topology Operator
			// kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v1.3.1/cert-manager.yaml
			// kubectl apply -f https://github.com/rabbitmq/messaging-topology-operator/releases/latest/download/messaging-topology-operator-with-certmanager.yaml
			LOG.warn("Missing RabbitMQ Cluster Operator. Later can be pre-installed like this:\n" +
					"kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v1.3.1/cert-manager.yaml \n" +
					"kubectl apply -f https://github.com/rabbitmq/messaging-topology-operator/releases/latest/download/messaging-topology-operator-with-certmanager.yaml");
			throw new ApiException("Install Messaging Topology Operator first!");
		}

		if (!isPodRunning("rabbitmq-system",
				String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)",
						"rabbitmq-cluster-operator", "rabbitmq-operator"))) {
			LOG.warn("The RabbitMQ Cluster Operator is Installed but still not running");
			throw new ApiException("RabbitMQ Cluster Operator is installed but still not Running");
		}

		if (!isPodRunning("rabbitmq-system",
				String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)",
						"messaging-topology-operator", "rabbitmq-operator"))) {
			LOG.warn("The Messaging Topology Operator is Installed but still not running");
			throw new ApiException("The Messaging Topology Operator is installed but still not Running");
		}

		LOG.info("Creating RabbitMQ Cluster Instance: {}/{}", namespace, ownerReference.getName());

		String multiYamlContent = "";
		try {
			multiYamlContent = StreamUtils.copyToString(rabbitmqService.getInputStream(), StandardCharsets.UTF_8);
			multiYamlContent = multiYamlContent.replaceAll("@@namespace@@", namespace);
			multiYamlContent = multiYamlContent.replaceAll("@@cluster-name@@", clusterStream.getMetadata().getName());
			multiYamlContent = multiYamlContent.replaceAll("@@queue-name@@", clusterStream.getSpec().getName());
			multiYamlContent = multiYamlContent.replaceAll("@@exchange-name@@", clusterStream.getSpec().getName());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Map<String, JsonObject> yamlResourceInstancesMap = yamlResourcesByKind(multiYamlContent);

		DynamicKubernetesObject newRmqClusterInstance = new DynamicKubernetesObject(
				yamlResourceInstancesMap.get("RabbitmqCluster"));
		newRmqClusterInstance.setMetadata(new V1ObjectMetaBuilder(newRmqClusterInstance.getMetadata())
				.withOwnerReferences(Collections.singletonList(ownerReference))
				.build());
		KubernetesApiResponse<DynamicKubernetesObject> createResponse = this.rmqClusterOperatorApi.create(
				namespace, newRmqClusterInstance, new CreateOptions());

		LOG.info("Creating RabbitMQ Queue Instance: {}/{}", namespace, ownerReference.getName());

		DynamicKubernetesObject newRmqQueueInstance = new DynamicKubernetesObject(
				yamlResourceInstancesMap.get("Queue"));
		newRmqQueueInstance.setMetadata(new V1ObjectMetaBuilder(newRmqQueueInstance.getMetadata())
				.withOwnerReferences(Collections.singletonList(ownerReference))
				.build());
		KubernetesApiResponse<DynamicKubernetesObject> createQueueResponse = this.rmqQueueOperatorApi.create(
				namespace, newRmqQueueInstance, new CreateOptions());

		DynamicKubernetesObject newRmqExchangeInstance = new DynamicKubernetesObject(
				yamlResourceInstancesMap.get("Exchange"));
		newRmqExchangeInstance.setMetadata(new V1ObjectMetaBuilder(newRmqExchangeInstance.getMetadata())
				.withOwnerReferences(Collections.singletonList(ownerReference))
				.build());
		KubernetesApiResponse<DynamicKubernetesObject> createExchangeResponse = this.rmqExchangeOperatorApi.create(
				namespace, newRmqExchangeInstance, new CreateOptions());

		DynamicKubernetesObject newRmqBindingInstance = new DynamicKubernetesObject(
				yamlResourceInstancesMap.get("Binding"));
		newRmqBindingInstance.setMetadata(new V1ObjectMetaBuilder(newRmqExchangeInstance.getMetadata())
				.withOwnerReferences(Collections.singletonList(ownerReference))
				.build());
		KubernetesApiResponse<DynamicKubernetesObject> bindingResponse = this.rmqBindingOperatorApi.create(
				namespace, newRmqBindingInstance, new CreateOptions());
	}

	private Map<String, JsonObject> yamlResourcesByKind(String multiYamlContent) {
		Map<String, JsonObject> map = new HashMap<>();

		for (String yaml : multiYamlContent.split("---")) {
			String json = AvroHelper.convertYamlOrJsonToJson(yaml);
			JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
			String kind = jsonObject.get("kind").getAsString(); // get property "message"
			map.put(kind, jsonObject);
		}
		return map;
	}

	@Override
	public boolean isRunning(V1OwnerReference ownerReference, String namespace) {
		return isPodRunning(namespace,
				String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (rabbitmq)",
						ownerReference.getName()));
	}

	@Override
	public String getStorageAddress(V1OwnerReference ownerReference, String namespace,
			boolean isServiceBindingEnabled) {
		try {
			RabbitConnectionInfo rci = this.getRabbitConnectionInfo(ownerReference, namespace, isServiceBindingEnabled);

			String rabbitCredentials = (isServiceBindingEnabled) ? ""
					: String.format(",\"username\": \"%s\", \"password\": \"%s\" ", rci.getUsername(),
							rci.getPassword());

			return String.format("" +
					"     \"production\": {" +
					"         \"url\": \"%s:%s\", " +
					"         \"protocol\": \"%s\", " +
					"         \"protocolVersion\": \"1.0.0\", " +
					"         \"variables\": { " +
					"              \"host\": \"%s\", " +
					"              \"port\": \"%s\" " +
					"               %s " +
					"           } " +
					"       }",
					rci.getHost(), rci.getPort(), getProtocolName(), rci.getHost(), rci.getPort(),
					rabbitCredentials);
		} catch (ApiException e) {
			LOG.warn(String.format("Cloud not retrieve the Rabbit Cluster [%s/%s] pod!", namespace,
					ownerReference.getName()), e);
			return "";
		}
	}

	private RabbitConnectionInfo getRabbitConnectionInfo(V1OwnerReference ownerReference, String namespace,
			boolean isServiceBindingEnabled)
			throws ApiException {
		// KubernetesApiResponse<DynamicKubernetesObject> rmqInstance =
		// rmqClusterOperatorApi.get(namespace,ownerReference.getName());
		List<V1Service> services = coreV1Api.listNamespacedService(namespace, null, null, null,
				String.format("metadata.name=%s", ownerReference.getName()),
				String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)",
						ownerReference.getName(), "rabbitmq"),
				null, null, null, null, null).getItems();

		// TODO handle NPE
		V1Service service = services.get(0);
		Integer rmqPort = service.getSpec().getPorts().stream()
				.filter(p -> "amqp".equalsIgnoreCase(p.getName()))
				.map(p -> p.getPort())
				.findFirst().get();
		String rmqHost = service.getSpec().getClusterIP();

		if (!isServiceBindingEnabled) {
			V1SecretList secrets = coreV1Api.listNamespacedSecret(namespace,
					null, null, null, null,
					String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (rabbitmq)",
							ownerReference.getName()),
					null, null, null, null, null);
			V1Secret rmqSecret = secrets.getItems().get(0);
			byte[] rmqUsername = rmqSecret.getData().get("username");
			byte[] rmqPassword = rmqSecret.getData().get("password");

			return new RabbitConnectionInfo(rmqHost, rmqPort, new String(rmqUsername), new String(rmqPassword));
		}
		return new RabbitConnectionInfo(rmqHost, rmqPort, null, null);
	}

	private boolean isPodExists(String name, String namespace, String component) {
		try {
			List<V1Pod> pods = findPods(namespace, null,
					String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)", name,
							component));
			return pods != null && pods.size() > 0;
		} catch (ApiException e) {
			LOG.error("Could not check for Pod existence.", e);
			return false;
		}
	}

	private boolean isPodRunning(String namespace, String labelSelector) {
		try {
			return findPods(namespace, "status.phase=Running", labelSelector).size() > 0;
		} catch (ApiException e) {
			e.printStackTrace();
		}
		return false;

	}

	private List<V1Pod> findPods(String namesapce, String fieldSelector, String labelSelector) throws ApiException {
		return coreV1Api.listNamespacedPod(namesapce, null, null, null, fieldSelector,
				labelSelector, null, null, null, null, null).getItems();
	}

	private static class RabbitConnectionInfo {

		private String host;
		private final Integer port;
		private final String username;
		private final String password;

		public RabbitConnectionInfo(String host, Integer port, String username, String password) {
			this.host = host;
			this.port = port;
			this.username = username;
			this.password = password;
		}

		String getHost() {
			return host;
		}

		void setHost(String host) {
			this.host = host;
		}

		Integer getPort() {
			return port;
		}

		String getUsername() {
			return username;
		}

		String getPassword() {
			return password;
		}
	}

	public static boolean isAddressReachable(String address, int port, int timeout) {
		try (Socket testSocket = new Socket()) {
			testSocket.connect(new InetSocketAddress(address, port), timeout);
			return true;
		} catch (IOException exception) {
			return false;
		}
	}

	private static Resource toResource(String uri) {
		return new DefaultResourceLoader().getResource(uri);
	}

}
