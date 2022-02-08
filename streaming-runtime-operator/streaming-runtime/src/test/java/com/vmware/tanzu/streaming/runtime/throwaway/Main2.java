package com.vmware.tanzu.streaming.runtime.throwaway;

import java.io.IOException;
import java.util.Map;

import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;

public class Main2 {
	public static void main(String[] args) throws IOException, KubectlException, ApiException {
		ApiClient apiClient = Config.defaultClient();

		Map<String, byte[]> secretData = getSecret(apiClient, "hello-world", "default").getData();
		//Map<String, byte[]> secretData = secretData(apiClient, "hello-world", "default");

		//System.out.println(secret.getData());
		secretData.entrySet().stream().forEach(e -> {
			//System.out.println(String.format("key: %s, value:%s", e.getKey(), Base64.getDecoder().decode(new String(e.getValue()))));
			System.out.println(String.format("[%s] = [%s]", e.getKey(), new String(e.getValue())));
		});

		System.out.println(new String(secretData.get("username")));

		DynamicKubernetesApi rmq = new DynamicKubernetesApi("rabbitmq.com",
				"v1beta1", "rabbitmqclusters", apiClient);

		KubernetesApiResponse<DynamicKubernetesObject> rmqInstance = rmq.get("hello-world", "default");
		System.out.println("Instance name= " + rmqInstance.getObject().getMetadata().getName());


		apiClient.getHttpClient().connectionPool().evictAll();
	}

	public static V1Secret getSecret(ApiClient apiClient,
			String rmqInstanceName, String rmqInstanceNamespace) throws ApiException {

		V1SecretList secrets = new CoreV1Api(apiClient).listNamespacedSecret(rmqInstanceNamespace,
				null, null, null, null,
				"app.kubernetes.io/name in (" + rmqInstanceName + "),app.kubernetes.io/component in (rabbitmq)"
				, null, null, null, null, null);
		return secrets.getItems().get(0);
	}

	public static Map<String, byte[]> secretData(ApiClient apiClient,
			String rmqInstanceName, String rmqInstanceNamespace) throws KubectlException {

		// get Username
		// kubectl get rabbitmqclusters  hello-world -o=jsonpath='Name: {.status.binding.name}'

		// Access the https://www.rabbitmq.com/kubernetes/operator resource
		DynamicKubernetesApi rmq = new DynamicKubernetesApi("rabbitmq.com",
				"v1beta1", "rabbitmqclusters", apiClient);

		KubernetesApiResponse<DynamicKubernetesObject> rmqInstance =
				rmq.get(rmqInstanceNamespace, rmqInstanceName);

		String rqmUserName = rmqInstance.getObject().getRaw()
				.get("status").getAsJsonObject()
				.get("binding").getAsJsonObject()
				.get("name").getAsString();


		String ns = rmqInstance.getObject().getMetadata().getNamespace();

		// Get secret and credentials for the user
		V1Secret secret = Kubectl.get(V1Secret.class)
				.name(rqmUserName)
				.namespace(ns)
				.apiClient(apiClient)
				.execute();

		return secret.getData();
	}

}
