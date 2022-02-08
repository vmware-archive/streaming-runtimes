package com.vmware.tanzu.streaming.runtime.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import com.vmware.tanzu.streaming.models.V1alpha1ClusterStream;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
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

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Alternative Rabbit Protocol Deployment Editor that uses the Rabbit K8s Operator internally.
 * (Experimental)
 */
@Component
public class RabbitMqOpDeploymentEditor implements ProtocolDeploymentEditor {

	private static final Logger LOG = LoggerFactory.getLogger(RabbitMqOpDeploymentEditor.class);

	private final ApiClient apiClient;
	private final CoreV1Api coreV1Api;
	private final DynamicKubernetesApi rmqClusterOperatorApi;
	private final Boolean enablePortForward;

	public RabbitMqOpDeploymentEditor(ApiClient apiClient, CoreV1Api coreV1Api,
			@Value("${streaming-runtime.enablePortForward}") Boolean enablePortForward) {
		this.apiClient = apiClient;
		this.coreV1Api = coreV1Api;
		this.rmqClusterOperatorApi = new DynamicKubernetesApi(
				"rabbitmq.com",
				"v1beta1",
				"rabbitmqclusters", apiClient);
		this.enablePortForward = enablePortForward;
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
	public void createIfNotFound(V1OwnerReference ownerReference, String namespace) throws ApiException {

		// Check if the RMQ pods are running
		if (isRunning(ownerReference, namespace)) {
			LOG.info("RabbitMQ Cluster Instance {}/{} already running!", namespace, ownerReference.getName());
			return;
		}

		// Check if the RMQ Cluster Instance is already created
		KubernetesApiResponse<DynamicKubernetesObject> existingRmqClusterInstance =
				rmqClusterOperatorApi.get(namespace, ownerReference.getName());
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
			throw new ApiException("Install the RabbitMQ Cluster Operator first");
		}

		if (!isPodRunning("rabbitmq-system", String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)",
				"rabbitmq-cluster-operator", "rabbitmq-operator"))) {
			LOG.warn("Install the RabbitMQ Cluster Operator is Installed but still not running");
			throw new ApiException("RabbitMQ Cluster Operator is installed but still not Running");
		}

		LOG.info("Creating RabbitMQ Cluster Instance: {}/{}", namespace, ownerReference.getName());

		DynamicKubernetesObject newRmqClusterInstance = new DynamicKubernetesObject();
		V1ObjectMeta metadata = new V1ObjectMetaBuilder()
				.withName(ownerReference.getName())
				.withOwnerReferences(Collections.singletonList(ownerReference))
				.withNamespace(namespace)
				.build();
		newRmqClusterInstance.setMetadata(metadata);
		newRmqClusterInstance.setKind("RabbitmqCluster");
		newRmqClusterInstance.setApiVersion("rabbitmq.com/v1beta1");

		KubernetesApiResponse<DynamicKubernetesObject> createResponse =
				rmqClusterOperatorApi.create(namespace, newRmqClusterInstance, new CreateOptions());
	}

	@Override
	public void postCreateConfiguration(V1OwnerReference ownerReference,
			String namespace, V1alpha1ClusterStream clusterStream) throws ApiException {

		if (!isRunning(ownerReference, namespace)) {
			throw new ApiException(String.format(
					"Failed to configure protocol: %s/%s! Not running Rabbit Cluster for ClusterStream %s/%s",
					getProtocolName(), getProtocolDeploymentEditorName(), namespace, ownerReference.getName()));
		}

		try {
			RabbitConnectionInfo rci = getRabbitConnectionInfo(ownerReference, namespace);

			Runnable addQueuesPerKey = () -> {
				CachingConnectionFactory connectionFactory = null;
				try {
					connectionFactory = new CachingConnectionFactory();
					connectionFactory.setHost(this.enablePortForward ? "localhost" : rci.getHost());
					connectionFactory.setPort(rci.getPort());
					connectionFactory.setUsername(rci.getUsername());
					connectionFactory.setPassword(rci.getPassword());

					RabbitAdmin admin = new RabbitAdmin(connectionFactory);

					if (clusterStream.getSpec().getKeys() != null) {
						for (String key : clusterStream.getSpec().getKeys()) {
							if (StringUtils.hasText(key)) {
								admin.declareQueue(new Queue(key));
							}
						}
					}
				}

				finally {
					if (connectionFactory != null) {
						connectionFactory.destroy();
					}
				}
			};

//			if (this.enablePortForward) {
//				V1Pod rmqPod = findPods(namespace, null,
//						String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)",
//								ownerReference.getName(), "rabbitmq")).get(0);
//
//				try (KubernetesPortForwardUtil pf = new KubernetesPortForwardUtil(
//						apiClient, rmqPod.getMetadata().getName(),
//						rmqPod.getMetadata().getNamespace(), rci.getPort(), rci.getPort())) {
//					rci.setHost("localhost");
//					pf.start();
//					addQueuesPerKey.run();
//				}
//			}
//			else {
//				addQueuesPerKey.run();
//			}

			addQueuesPerKey.run();
		}
		catch (Throwable t) {
			throw new ApiException(String.format(
					"Failed to configure protocol: %s/%s for ClusterStream %s/%s",
					getProtocolName(), getProtocolDeploymentEditorName(), namespace, ownerReference.getName()),
					t, -1, null, null);
		}
	}

	@Override
	public boolean isRunning(V1OwnerReference ownerReference, String namespace) {
		return isPodRunning(namespace,
				String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (rabbitmq)",
						ownerReference.getName()));
	}

	@Override
	public String getStorageAddress(V1OwnerReference ownerReference, String namespace) {
		try {
			RabbitConnectionInfo rci = getRabbitConnectionInfo(ownerReference, namespace);
			return String.format("" +
							"     \"production\": {" +
							"         \"url\": \"%s:%s\", " +
							"         \"protocol\": \"%s\", " +
							"         \"protocolVersion\": \"1.0.0\", " +
							"         \"variables\": { " +
							"              \"host\": \"%s\", " +
							"              \"port\": \"%s\", " +
							"              \"username\": \"%s\", " + // TODO: exposed secrets!
							"              \"password\": \"%s\" " +
							"           } " +
							"       }",
					rci.getHost(), rci.getPort(), getProtocolName(), rci.getHost(), rci.getPort(),
					rci.getUsername(), rci.getPassword());
		}
		catch (ApiException e) {
			LOG.warn(String.format("Cloud not retrieve the Rabbit Cluster [%s/%s] pod!", namespace, ownerReference.getName()), e);
			return "";
		}
	}

	private RabbitConnectionInfo getRabbitConnectionInfo(V1OwnerReference ownerReference, String namespace) throws ApiException {
//		KubernetesApiResponse<DynamicKubernetesObject> rmqInstance = rmqClusterOperatorApi.get(namespace,ownerReference.getName());
		List<V1Service> services = coreV1Api.listNamespacedService(namespace, null, null, null,
				String.format("metadata.name=%s", ownerReference.getName()),
				String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)", ownerReference.getName(), "rabbitmq"),
				null, null, null, null, null).getItems();

		// TODO handle NPE
		V1Service service = services.get(0);
		Integer rmqPort = service.getSpec().getPorts().stream()
				.filter(p -> "amqp".equalsIgnoreCase(p.getName()))
				.map(p -> p.getPort())
				.findFirst().get();
		String rmqHost = service.getSpec().getClusterIP();

		V1SecretList secrets = coreV1Api.listNamespacedSecret(namespace,
				null, null, null, null,
				String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (rabbitmq)", ownerReference.getName())
				, null, null, null, null, null);
		V1Secret rmqSecret = secrets.getItems().get(0);
		byte[] rmqUsername = rmqSecret.getData().get("username");
		byte[] rmqPassword = rmqSecret.getData().get("password");

		return new RabbitConnectionInfo(rmqHost, rmqPort, new String(rmqUsername), new String(rmqPassword));
	}


	private boolean isPodExists(String name, String namespace, String component) {
		try {
			List<V1Pod> pods = findPods(namespace, null,
					String.format("app.kubernetes.io/name in (%s),app.kubernetes.io/component in (%s)", name, component));
			return pods != null && pods.size() > 0;
		}
		catch (ApiException e) {
			LOG.error("Could not check for Pod existence.", e);
			return false;
		}
	}

	private boolean isPodRunning(String namespace, String labelSelector) {
		try {
			return findPods(namespace, "status.phase=Running", labelSelector).size() > 0;
		}
		catch (ApiException e) {
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
		}
		catch (IOException exception) {
			return false;
		}
	}
}
