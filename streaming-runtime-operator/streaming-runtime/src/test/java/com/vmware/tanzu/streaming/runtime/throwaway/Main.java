package com.vmware.tanzu.streaming.runtime.throwaway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.KubectlPortForward;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class Main {
	public static void main(String[] args) throws IOException, KubectlException, ApiException {
		ApiClient apiClient = Config.defaultClient();

		String rmqPodName = "hello-world";
		String rmqPodNamespace = "default";
		V1PodList pod = new CoreV1Api(apiClient).listNamespacedPod(rmqPodNamespace,
				null, null, null, null,
				"app.kubernetes.io/name in (" + rmqPodName + "),app.kubernetes.io/component in (rabbitmq)"
				, null, null, null, null, null);

		String rmqServerInstanceName = pod.getItems().get(0).getMetadata().getName();
		String rmqServerInstanceNamespace = pod.getItems().get(0).getMetadata().getNamespace();
		KubectlPortForward portForward = Kubectl.portforward()
				.apiClient(apiClient)
				.name(rmqServerInstanceName) // hello-world-server-0
				.namespace(rmqServerInstanceNamespace) // default
				.ports(5672, 5672);

		Thread tt = new Thread(() -> {
			try {
				portForward.execute();
			}
			catch (KubectlException e) {
				e.printStackTrace();
			}
		});

		tt.start();


		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setHost("localhost"); connectionFactory.setUsername("default_user_RcN4pPS9X_EtUFRZCsV");
		connectionFactory.setPassword("tT5S92H9XcIttILZQ1bQx5VMJiw6AHJ7");

		RabbitAdmin admin = new RabbitAdmin(connectionFactory); admin.declareQueue(new Queue("myqueue"));

		//admin.declareExchange(new Exchange() {});

		AmqpTemplate template = new RabbitTemplate(connectionFactory); template.convertAndSend("myqueue", "foo");
		String foo = (String) template.receiveAndConvert("myqueue"); System.out.println("Foo = " + foo);


		System.out.println("Reachable: " + isAddressReachable("127.0.0.1", 5672, 2000));

		portForward.shutdown();

		apiClient.getHttpClient().connectionPool().evictAll();
	}

	private static boolean isAddressReachable(String address, int port, int timeout) {
		try (Socket testSocket = new Socket()) {
			testSocket.connect(new InetSocketAddress(address, port), timeout); return true;
		}
		catch (IOException exception) {
			//exception.printStackTrace();
			return false;
		}
	}
}
