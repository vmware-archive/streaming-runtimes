package com.vmware.tanzu.streaming.runtime.throwaway;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vmware.tanzu.streaming.runtime.dataschema.AvroHelper;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1OwnerReferenceBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;

public class Main3 {

	private static final Resource yamlResource = toResource(
			"file:/Users/ctzolov/Dev/projects/tanzu/streaming-runtimes/streaming-runtime-operator/streaming-runtime/src/main/resources/manifests/protocol/rabbitmq-op/mrq-queue.yaml");

	public static void main(String[] args) throws IOException, KubectlException, ApiException {

		String multiYamlContent = StreamUtils.copyToString(yamlResource.getInputStream(), Charset.defaultCharset());

		Map<String, JsonObject> map = new HashMap<>();

		V1OwnerReference ownerReference = new V1OwnerReferenceBuilder()
			.build();

		for (String yaml : multiYamlContent.split("---")) {
			String json = AvroHelper.convertYamlOrJsonToJson(yaml);
			JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
			String kind = jsonObject.get("kind").getAsString(); // get property "message"

			System.out.println(kind);
			map.put(kind, jsonObject);

			DynamicKubernetesObject newRmqClusterInstance = new DynamicKubernetesObject(jsonObject);
			newRmqClusterInstance.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));

			System.out.println(newRmqClusterInstance.getMetadata().getNamespace());
		}

		// JsonObject childObject = rootObject.getAsJsonObject("place"); // get place
		// object
		// String place = childObject.get("name").getAsString(); // get property "name"
		// System.out.println(message + " " + place);

		ApiClient apiClient = Config.defaultClient();
	}

	private static Resource toResource(String uri) {
		return new DefaultResourceLoader().getResource(uri);
	}
}
