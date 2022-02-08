package com.vmware.tanzu.streaming.runtime;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@Component
public class ConfigMapUpdater {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigMapUpdater.class);

	private final CoreV1Api coreV1Api;
	private final Lister<V1ConfigMap> configMapLister;
	private final ObjectMapper yamlMapper;

	public ConfigMapUpdater(CoreV1Api coreV1Api, Lister<V1ConfigMap> configMapLister,
			ObjectMapper yamlMapper) {
		this.coreV1Api = coreV1Api;
		this.configMapLister = configMapLister;
		this.yamlMapper = yamlMapper;
	}

	public V1ConfigMap createConfigMap(V1OwnerReference ownerReference, String configMapName, String configMapNamespace,
			String configMapKey, String serializedContent) throws ApiException {
		LOG.debug("Creating config map {}/{}", configMapNamespace, ownerReference.getName());
		V1ConfigMap configMap = new V1ConfigMap()
				.apiVersion("v1")
				.kind("ConfigMap")
				.metadata(new V1ObjectMeta()
						.name(configMapName)
						.namespace(configMapNamespace)
						.ownerReferences(singletonList(ownerReference)));
		addDataToConfigMap(configMap, configMapKey, serializedContent);
		return coreV1Api.createNamespacedConfigMap(configMapNamespace, configMap, null, null, null);
	}

//	public V1ConfigMap removeStream(String streamNameToRemove, String clusterStreamName) throws ApiException, JsonProcessingException {
//		LOG.debug("Removing Stream {} from config map {}", streamNameToRemove, clusterStreamName);
//		StreamsProperties properties = getExistingProperties(clusterStreamName);
//		properties.getStreams().removeIf(stream -> stream.getName().equalsIgnoreCase(streamNameToRemove));
//		return updateConfigMap(clusterStreamName, properties);
//	}

	public Map<String, Object> getExistingProperties(String configMapKey, String configMapName, String configMapNamespace) throws JsonProcessingException {
		V1ConfigMap configMap = getExistingConfigMap(configMapName, configMapNamespace);
		String serializedStreams = configMap.getData().get(configMapKey);
		Map<String, Object> properties = yamlMapper.readValue(serializedStreams, Map.class);
		return properties;
	}

//	public boolean isStreamExist(String streamName, String clusterStreamName) {
//		try {
//			return getExistingProperties(clusterStreamName).getStreams().stream()
//					.anyMatch(stream -> stream.getName().equalsIgnoreCase(streamName));
//		}
//		catch (JsonProcessingException e) {
//			e.printStackTrace();
//		}
//		return false;
//	}

	public V1ConfigMap updateConfigMap(String configMapName, String configMapNamespace, String configMapKey, String serializedContent) throws ApiException {
		V1ConfigMap configMap = addDataToConfigMap(getExistingConfigMap(configMapName, configMapNamespace), configMapKey, serializedContent);
		return coreV1Api.replaceNamespacedConfigMap(configMap.getMetadata().getName(), configMapNamespace,
				configMap, null, null, null);
	}

	private V1ConfigMap addDataToConfigMap(V1ConfigMap configMap, String configMapKey, String serializedContent) {
		return configMap.data(singletonMap(configMapKey, serializedContent));
	}

	public boolean configMapExists(String configMapName, String configMapNamespace) {
		LOG.debug("Checking if config map {}/{} exists", configMapNamespace, configMapName);
		return (getExistingConfigMap(configMapName, configMapNamespace) != null);
	}

	private V1ConfigMap getExistingConfigMap(String configMapName, String configMapNamespace) {
		return configMapLister.namespace(configMapNamespace).get(configMapName);
	}
}
