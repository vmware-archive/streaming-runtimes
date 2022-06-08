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

package com.vmware.tanzu.streaming.runtime.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamStatusStorageAddressServer;
import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.runtime.ProcessorStatusException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

public abstract class AbstractScsProcessorAdapter implements ProcessorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractScsProcessorAdapter.class);
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    protected static final Resource PROCESSOR_DEPLOYMENT_TEMPLATE = toResource(
            "classpath:manifests/processor/generic-streaming-runtime-processor-deployment.yaml");
    protected static final Resource PROCESSOR_STATEFULSET_SERVICE_TEMPLATE = toResource(
            "classpath:manifests/processor/statefulset-service-template.yaml");
    protected static final Resource PROCESSOR_STATEFULSET_TEMPLATE = toResource(
            "classpath:manifests/processor/statefulset-template.yaml");

    protected final ObjectMapper yamlMapper;
    protected final AppsV1Api appsV1Api;
    protected final CoreV1Api coreV1Api;

    public AbstractScsProcessorAdapter(ObjectMapper yamlMapper, AppsV1Api appsV1Api, CoreV1Api coreV1Api) {
        this.yamlMapper = yamlMapper;
        this.appsV1Api = appsV1Api;
        this.coreV1Api = coreV1Api;
    }

    protected static Resource toResource(String uri) {
        return new DefaultResourceLoader().getResource(uri);
    }

    @Override
    public void createProcessorDeployment(V1alpha1Processor processor, V1OwnerReference ownerReference,
            List<V1alpha1Stream> inputStreams, List<V1alpha1Stream> outputStreams)
            throws IOException, ApiException, ProcessorStatusException {

        LOG.debug("Creating deployment {}/{}", processor.getMetadata().getNamespace(), ownerReference.getName());

        // Env variables
        Map<String, String> envs = new HashMap<>();

        V1alpha1Stream inputStream = this.getSingleOrNull(inputStreams);
        V1alpha1Stream outputStream = this.getSingleOrNull(outputStreams);

        if (inputStream != null) {
            V1alpha1ClusterStreamStatusStorageAddressServer inServer = inputStream.getStatus()
                    .getStorageAddress().getServer().values().iterator().next();

            if (inServer.getProtocol().equalsIgnoreCase("kafka")) {
                envs.put("SPRING_CLOUD_STREAM_BINDINGS_INPUT_BINDER", "kafka");
                envs.put("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS", inServer.getVariables().get("brokers"));
                envs.put("SPRING_CLOUD_STREAM_KAFKA_BINDER_ZKNODES", inServer.getVariables().get("zkNodes"));
            } else if (inServer.getProtocol().equalsIgnoreCase("rabbitmq")) {
                envs.put("SPRING_CLOUD_STREAM_BINDINGS_INPUT_BINDER", "rabbit");
                envs.put("SPRING_RABBITMQ_HOST", inServer.getVariables().get("host"));
                envs.put("SPRING_RABBITMQ_PORT", inServer.getVariables().get("port"));

                if (!StringUtils.hasText(inputStream.getSpec().getBinding())) {
                    envs.put("SPRING_RABBITMQ_USERNAME", inServer.getVariables().get("username"));
                    envs.put("SPRING_RABBITMQ_PASSWORD", inServer.getVariables().get("password"));
                }
            }
            envs.put("SPRING_CLOUD_STREAM_BINDINGS_INPUT_DESTINATION", inputStream.getSpec().getName()); // TODO
            envs.put("SPRING_CLOUD_STREAM_FUNCTION_BINDINGS_PROXY-IN-0", "input"); // TODO
        }

        if (outputStream != null) {

            V1alpha1ClusterStreamStatusStorageAddressServer outServer = outputStream.getStatus()
                    .getStorageAddress().getServer().values().iterator().next();

            if (outServer.getProtocol().equalsIgnoreCase("kafka")) {
                envs.put("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_BINDER", "kafka");
                envs.put("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS", outServer.getVariables().get("brokers"));
                envs.put("SPRING_CLOUD_STREAM_KAFKA_BINDER_ZKNODES", outServer.getVariables().get("zkNodes"));
            } else if (outServer.getProtocol().equalsIgnoreCase("rabbitmq")) {
                envs.put("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_BINDER", "rabbit");
                envs.put("SPRING_RABBITMQ_HOST", outServer.getVariables().get("host"));
                envs.put("SPRING_RABBITMQ_PORT", outServer.getVariables().get("port"));

                if (!StringUtils.hasText(outputStream.getSpec().getBinding())) {
                    envs.put("SPRING_RABBITMQ_USERNAME", outServer.getVariables().get("username"));
                    envs.put("SPRING_RABBITMQ_PASSWORD", outServer.getVariables().get("password"));
                }
            }
            envs.put("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_DESTINATION", outputStream.getSpec().getName()); // TODO
            envs.put("SPRING_CLOUD_STREAM_FUNCTION_BINDINGS_PROXY-OUT-0", "output"); // TODO
        }

        String partitionedInput = getProcessorAttribute(processor, "partitionedInput");
        
        if (StringUtils.hasText(partitionedInput) && "true".equalsIgnoreCase(partitionedInput)) {
            createStatefulSet(processor, ownerReference, envs);
        } else {
            createDeployment(processor, ownerReference, envs);            
        }
    }

    protected String getProcessorAttribute(V1alpha1Processor processor, String attributeName) {
        if (processor.getSpec().getAttributes() != null) {
            if (processor.getSpec().getAttributes().containsKey(attributeName)) {
                return processor.getSpec().getAttributes().get(attributeName);
            }
        }
        return null;
    }

    protected abstract List<V1Container> doAddContainers(V1alpha1Processor processor, Map<String, String> envs)
            throws ApiException;

    protected V1alpha1Stream getSingleOrNull(List<V1alpha1Stream> streams) {
        if (streams.size() > 0) {
            return streams.get(0);
        }
        return null;
    }

    protected void createDeployment(V1alpha1Processor processor, V1OwnerReference ownerReference,
            Map<String, String> envs) throws StreamReadException, DatabindException, IOException, ApiException {

        // Deployment
        V1Deployment body = this.yamlMapper.readValue(PROCESSOR_DEPLOYMENT_TEMPLATE.getInputStream(),
                V1Deployment.class);
        body.getMetadata().setName("srp-" + ownerReference.getName());
        body.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
        body.getSpec().getTemplate().getMetadata().getLabels().put("streaming-runtime", ownerReference.getName());

        body.getSpec().getTemplate().setSpec(new V1PodSpec());
        body.getSpec().getTemplate().getSpec().setContainers(new ArrayList<>());
        if (processor.getSpec().getReplicas() != null && processor.getSpec().getReplicas() != 1) {
            body.getSpec().setReplicas(processor.getSpec().getReplicas());
        }

        // Add additional containers
        body.getSpec().getTemplate().getSpec().getContainers().addAll(this.doAddContainers(processor, envs));

        this.appsV1Api.createNamespacedDeployment(
                processor.getMetadata().getNamespace(), body, null, null, null);
    }

    protected void createStatefulSet(V1alpha1Processor processor, V1OwnerReference ownerReference,
            Map<String, String> envs) throws StreamReadException, DatabindException, IOException, ApiException {

        V1StatefulSet body = this.yamlMapper.readValue(PROCESSOR_STATEFULSET_TEMPLATE.getInputStream(),
                V1StatefulSet.class);

        body.getMetadata().setName("srp-" + ownerReference.getName());
        body.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
        body.getSpec().getTemplate().getMetadata().getLabels().put("streaming-runtime", ownerReference.getName());

        body.getSpec().getTemplate().setSpec(new V1PodSpec());
        body.getSpec().getTemplate().getSpec().setContainers(new ArrayList<>());
        if (processor.getSpec().getReplicas() != null && processor.getSpec().getReplicas() != 1) {
            body.getSpec().setReplicas(processor.getSpec().getReplicas());
        }
        body.getSpec().setServiceName("srp-" + ownerReference.getName());

        body.getSpec().getTemplate().getSpec().addInitContainersItem(
                new V1ContainerBuilder().withName("index-provider")
                        .withImage("busybox:1.35.0")
                        .withCommand("sh", "-c",
                                "echo INSTANCE_INDEX=\"$(expr $HOSTNAME | grep -o \"[[:digit:]]*$\")\" >> /config/application.properties && echo spring.cloud.stream.instance-index=\"$(expr $HOSTNAME | grep -o \"[[:digit:]]*$\")\" >> /config/application.properties")
                        .withVolumeMounts(
                                new V1VolumeMountBuilder().withName("config").withMountPath("/config").build())
                        .build());

        // Add additional containers
        body.getSpec().getTemplate().getSpec().getContainers().addAll(this.doAddContainers(processor, envs));
        for (V1Container container : body.getSpec().getTemplate().getSpec().getContainers()) {
            container.addVolumeMountsItem(
                    new V1VolumeMountBuilder().withName("config").withMountPath("/config").build());
        }

        this.createService(ownerReference, PROCESSOR_STATEFULSET_SERVICE_TEMPLATE,
                processor.getMetadata().getNamespace());

        this.appsV1Api.createNamespacedStatefulSet(
                processor.getMetadata().getNamespace(), body, null, null, null);
    }

    protected V1Service createService(V1OwnerReference ownerReference,
            Resource serviceYaml, String appNamespace) throws ApiException {

        try {
            LOG.debug("Creating service {}/{}", appNamespace, ownerReference.getName());
            V1Service body = yamlMapper.readValue(serviceYaml.getInputStream(), V1Service.class);
            body.getMetadata().setName("srp-" + ownerReference.getName());
            body.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
            body.getMetadata().getLabels().put("streaming-runtime", ownerReference.getName());

            return coreV1Api.createNamespacedService(appNamespace, body, null, null, null);

        } catch (IOException ioe) {
            throw new ApiException(ioe);
        } catch (ApiException apiException) {
            if (apiException.getCode() == 409) {
                LOG.info("Required service is already deployed: " + ownerReference.getName());
                return null;
            }
            throw apiException;
        }
    }
}
