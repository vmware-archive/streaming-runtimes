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
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStream;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class KafkaDeploymentEditor implements ProtocolDeploymentEditor {

	private static final Logger LOG = LoggerFactory.getLogger(KafkaDeploymentEditor.class);

	private final CoreV1Api coreV1Api;
	private final AppsV1Api appsV1Api;
	private final ObjectMapper yamlMapper;

	private static final Resource zkService = toResource("classpath:manifests/protocol/kafka/kafka-zk-svc.yaml");
	private static final Resource zkDeployment = toResource("classpath:manifests/protocol/kafka/kafka-zk-deployment.yaml");
	private static final Resource kafkaService = toResource("classpath:manifests/protocol/kafka/kafka-svc.yaml");
	private static final Resource kafkaDeployment = toResource("classpath:manifests/protocol/kafka/kafka-deployment.yaml");
	private static final Resource schemaRegistryDeployment = toResource("classpath:manifests/protocol/kafka/kafka-schema-registry-deployment.yaml");
	private static final Resource schemaRegistryService = toResource("classpath:manifests/protocol/kafka/kafka-schema-registry-svc.yaml");
	private static final Resource kafkaUiService = toResource("classpath:manifests/protocol/kafka/kafka-kowl-ui-svc.yaml");
	private static final Resource kafkaUiDeployment = toResource("classpath:manifests/protocol/kafka/kafka-kowl-ui-deployment.yaml");

	public KafkaDeploymentEditor(CoreV1Api coreV1Api, AppsV1Api appsV1Api, ObjectMapper yamlMapper) {
		this.coreV1Api = coreV1Api;
		this.appsV1Api = appsV1Api;
		this.yamlMapper = yamlMapper;
	}

	@Override
	public String getProtocolName() {
		return "kafka";
	}

	@Override
	public void createIfNotFound(V1OwnerReference ownerReference, String namespace, V1alpha1ClusterStream clusterStream) throws ApiException {

		if (CollectionUtils.isEmpty(findServices(namespace, null, "app=kafka"))) {
			this.createService(ownerReference, zkService, namespace);
			this.createService(ownerReference, kafkaService, namespace);
			this.createService(ownerReference, schemaRegistryService, namespace);
			this.createService(ownerReference, kafkaUiService, namespace);
		}

		if (CollectionUtils.isEmpty(findPods(namespace, null, "app=kafka-zk,component=kafka-zk"))) {
			this.createDeployment(ownerReference, zkDeployment, namespace, "digitalwonderland/zookeeper");
		}

		if (CollectionUtils.isEmpty(findPods(namespace, null, "app=kafka,component=kafka-broker"))) {
			this.createDeployment(ownerReference, kafkaDeployment, namespace, "digitalwonderland/zookeeper");
		}

		if (CollectionUtils.isEmpty(findPods(namespace, null, "app=s-registry,component=s-registry"))) {
			this.createDeployment(ownerReference, schemaRegistryDeployment, namespace, "");
		}

		if (CollectionUtils.isEmpty(findPods(namespace, null, "app=kafka-kowl-ui,component=kafka-kowl-ui"))) {
			this.createDeployment(ownerReference, kafkaUiDeployment, namespace, "");
		}
	}

	@Override
	public boolean isRunning(V1OwnerReference ownerReference, String namespace) {
		try {
			int size = findPods(namespace, "status.phase=Running", "app in (kafka,kafka-zk,s-registry)").size();
			return size == 3;
		}
		catch (ApiException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String getStorageAddress(V1OwnerReference ownerReference, String namespace, boolean isServiceBindingEnabled) {

		return "" +
				"     \"production\": {" +
				"         \"url\": \"localhost:8080\", " +
				"         \"protocol\": \"kafka\", " +
				"         \"protocolVersion\": \"1.0.0\", " +
				"         \"variables\": { " +
				"              \"brokers\": \"kafka." + namespace + ".svc.cluster.local:9092\", " +
				"              \"zkNodes\": \"kafka-zk." + namespace + ".svc.cluster.local:2181\", " +
				"              \"schemaRegistry\": \"http://s-registry." + namespace + ".svc.cluster.local:8081\" " +
				"           } " +
				"       }";
	}

	private List<V1Pod> findPods(String namesapce, String fieldSelector, String labelSelector) throws ApiException {
		return coreV1Api.listNamespacedPod(namesapce, null, null, null, fieldSelector,
				labelSelector, null, null, null, null, null).getItems();
	}

	private List<V1Service> findServices(String namesapce, String fieldSelector, String selector) throws ApiException {
		return coreV1Api.listNamespacedService(namesapce, null, null, null, fieldSelector,
				selector, null, null, null, null, null).getItems();
	}

	private V1Deployment createDeployment(V1OwnerReference ownerReference,
			Resource deploymentYaml, String appNamespace,
			String appImage) throws ApiException {
		try {
			LOG.debug("Creating deployment {}/{}", appNamespace, ownerReference.getName());
			V1Deployment body = yamlMapper.readValue(deploymentYaml.getInputStream(), V1Deployment.class);
			//body.getMetadata().setName(ownerReference.getName());
			body.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
			//body.getSpec().getSelector().getMatchLabels().put("app", ownerReference.getName());
			//body.getSpec().getTemplate().getMetadata().getLabels().put("app", ownerReference.getName());
//		body.getSpec().getTemplate().getSpec().getVolumes().get(0).getConfigMap().setName(ownerReference.getName());
//		body.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(appImage);
			body.getSpec().getTemplate().getMetadata().getLabels().put("streaming-runtime", ownerReference.getName());


			return appsV1Api.createNamespacedDeployment(appNamespace, body, null, null, null);
		}
		catch (IOException ioException) {
			throw new ApiException(ioException);
		}
		catch (ApiException apiException) {
			if (apiException.getCode() == 409) {
				LOG.info("Required deployment is already deployed: " + ownerReference.getName());
				return null;
			}
			throw apiException;
		}
	}

	private V1Service createService(V1OwnerReference ownerReference,
			Resource serviceYaml, String appNamespace) throws ApiException {
		try {
			LOG.debug("Creating service {}/{}", appNamespace, ownerReference.getName());
			V1Service body = yamlMapper.readValue(serviceYaml.getInputStream(), V1Service.class);
			//body.getMetadata().setName(ownerReference.getName());
			body.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
			//body.getSpec().getSelector().getMatchLabels().put("app", ownerReference.getName());
			body.getMetadata().getLabels().put("streaming-runtime", ownerReference.getName());

			return coreV1Api.createNamespacedService(appNamespace, body, null, null, null);
		}
		catch (IOException ioe) {
			throw new ApiException(ioe);
		}
		catch (ApiException apiException) {
			if (apiException.getCode() == 409) {
				LOG.info("Required service is already deployed: " + ownerReference.getName());
				return null;
			}
			throw apiException;
		}

	}

	private static Resource toResource(String uri) {
		return new DefaultResourceLoader().getResource(uri);
	}
}
