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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import com.vmware.tanzu.streaming.models.V1alpha1ProcessorSpecTemplateSpecContainers;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.runtime.dataschema.DataSchemaProcessingContext;
import com.vmware.tanzu.streaming.runtime.dataschema.WatermarkExpression;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * srp.spel.expression -
 * 
 * srp.envs: - Adds random environment variable to the SRP container configuration. Example: "TEST_BAR=FOO;TEST_FOO=BAR"
 * 
 * srp.output.headers - Adds headers to the output messages. Uses expression like:
 * "user=header.fullName;team=payload.teamName"
 * 
 * srp.window - Defines the Time-Window aggregation interval. Examples: 5s, 2m, 1h
 * 
 * srp.window.idle.timeout - Defines an interval of inactivity to release the idle windows. Should be larger than the
 * window interval! Example: 2m
 * 
 * srp.input.timestampExpression: JsonPath expression Example: header.eventtime, or payload.score_time
 * 
 * srp.maxOutOfOrderness: 5s srp.allowedLateness: 2h
 * 
 * srp.lateEventMode - Defines the policy to deal with late event records. Supports: DROP, UPSERT, SIDE_CHANNEL modes
 * and defaults to DROP.
 * 
 * srp.input.schemaRegistryUri - configure the Schema Registry uri. Required for Avro content types.
 * 
 * srp.skipUdf - Forcefully disables the the UDF call. Defaults to false. Note that if you don't provide side-car
 * container this would effectively skip the UDF.
 */
@Component
public class SrpProcessorAdapter extends AbstractScsProcessorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractScsProcessorAdapter.class);

    private static final Resource SRP_PROCESSOR_CONTAINER_TEMPLATE = toResource(
            "classpath:manifests/processor/srp-processor-container-template.yaml");

    public SrpProcessorAdapter(ObjectMapper yamlMapper, AppsV1Api appsV1Api, CoreV1Api coreV1Api) {
        super(yamlMapper, appsV1Api, coreV1Api);
    }

    @Override
    public String type() {
        return "SRP";
    }

    private Map<String, String> parseKeyValueExpression(String keyValueExpression) {
        Map<String, String> result = new HashMap<>();

        if (StringUtils.hasText(keyValueExpression)) {
            String[] pairs = keyValueExpression.split(";");
            if (pairs != null) {
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue != null && keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        result.put(key, value);
                    }
                    else {
                        LOG.warn("Invalid key/value expression: " + pair + " , part of: " + keyValueExpression);
                    }
                }
            }
        }

        return result;
    }

    @Override
    protected void doAdditionalConfigurations(V1alpha1Processor processor, V1alpha1Stream inputStream,
            V1alpha1Stream outputStream, Map<String, String> envs) {

        // TODO perhaps the native encoding/decoding should be applied to Kafka only?
        envs.put("SPRING_CLOUD_STREAM_BINDINGS_INPUT_CONSUMER_USENATIVEDECODING", "true");
        envs.put("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_PRODUCER_USENATIVEENCODING", "true");

        envs.put("SRP_PROCESSOR_NAME", processor.getMetadata().getName());

        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.spel.expression"))) {
            envs.put("SRP_PROCESSOR_ENABLESPELTRANSFORMATION", "true");
            envs.put("SPEL_FUNCTION_EXPRESSION", this.getProcessorAttribute(processor, "srp.spel.expression"));
        }

        parseKeyValueExpression(this.getProcessorAttribute(processor, "srp.envs")).entrySet().stream()
                .forEach(entry -> envs.put(entry.getKey(), entry.getValue()));

        parseKeyValueExpression(this.getProcessorAttribute(processor, "srp.output.headers")).entrySet().stream()
                .forEach(entry -> envs.put("SRP_PROCESSOR_OUTPUT_HEADERS_" + entry.getKey(), entry.getValue()));

        String windowAttribute = this.getProcessorAttribute(processor, "srp.window");
        if (StringUtils.hasText(windowAttribute)) {
            envs.put("SRP_PROCESSOR_WINDOW", windowAttribute);
            envs.put("SRP_PROCESSOR_SKIPAGGREGATION", "false");
        }
        else {
            envs.put("SRP_PROCESSOR_SKIPAGGREGATION", "true");
        }

        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.window.idle.timeout"))) {
            envs.put("SRP_PROCESSOR_IDLEWINDOWTIMEOUT",
                    this.getProcessorAttribute(processor, "srp.window.idle.timeout"));
        }

        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.lateEventMode"))) {
            envs.put("SRP_PROCESSOR_LATEEVENTMODE",
                    this.getProcessorAttribute(processor, "srp.lateEventMode"));
        }

        if (inputStream != null && inputStream.getSpec().getDataSchemaContext() != null) {
            try {
                DataSchemaProcessingContext inputSchemaContext = DataSchemaProcessingContext.of(inputStream);
                if (StringUtils.hasText(inputSchemaContext.getSchemaRegistryUri())) {
                    envs.put("SRP_PROCESSOR_INPUT_SCHEMAREGISTRYURI", inputSchemaContext.getSchemaRegistryUri());
                }
                Map<String, String> timeAttributes = inputSchemaContext.getTimeAttributes();
                if (!CollectionUtils.isEmpty(timeAttributes)) {
                    String timeAttributeName = timeAttributes.keySet().iterator().next();
                    String watermark = timeAttributes.get(timeAttributeName);

                    if (StringUtils.hasText(watermark)) {
                        WatermarkExpression watermarkExpression = WatermarkExpression.of(watermark);
                        envs.put("SRP_PROCESSOR_MAXOUTOFORDERNESS",
                                watermarkExpression.getOutOfOrderness().toSeconds() + "s");
                        envs.put("SRP_PROCESSOR_INPUT_TIMESTAMPEXPRESSION",
                                watermarkExpression.getEventTimeFieldName());
                    }
                    else {
                        envs.put("SRP_PROCESSOR_INPUT_TIMESTAMPEXPRESSION", timeAttributeName);
                    }
                }
            }
            catch (ApiException e) {
                e.printStackTrace();
            }
        }

        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.input.timestampExpression"))) {
            envs.put("SRP_PROCESSOR_INPUT_TIMESTAMPEXPRESSION",
                    this.getProcessorAttribute(processor, "srp.input.timestampExpression"));
        }

        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.maxOutOfOrderness"))) {
            envs.put("SRP_PROCESSOR_MAXOUTOFORDERNESS",
                    this.getProcessorAttribute(processor, "srp.maxOutOfOrderness"));
        }
        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.allowedLateness"))) {
            envs.put("SRP_PROCESSOR_ALLOWEDLATENESS", this.getProcessorAttribute(processor, "srp.allowedLateness"));
        }

        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.input.schemaRegistryUri"))) {
            envs.put("SRP_PROCESSOR_INPUT_SCHEMAREGISTRYURI",
                    this.getProcessorAttribute(processor, "srp.input.schemaRegistryUri"));
        }

        // Instruct SR Processor to skip UDF if such not defined.
        if (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.skipUdf"))) {
            envs.put("SRP_PROCESSOR_SKIPUDF", this.getProcessorAttribute(processor, "srp.skipUdf"));
        }
        else if (processor.getSpec().getTemplate() == null || processor.getSpec().getTemplate().getSpec() == null
                || processor.getSpec().getTemplate().getSpec().getContainers() == null
                || processor.getSpec().getTemplate().getSpec().getContainers().size() == 0) {

            envs.put("SRP_PROCESSOR_SKIPUDF", "true");
        }
        else {
            envs.put("SRP_PROCESSOR_SKIPUDF", "false");
        }

        String clientGrpcPort = (StringUtils.hasText(this.getProcessorAttribute(processor, "srp.grpcPort")))
                ? this.getProcessorAttribute(processor, "srp.grpcPort")
                : "55554";

        envs.put("SPRING_CLOUD_FUNCTION_GRPC_PORT", clientGrpcPort);
    }

    @Override
    protected List<V1Container> doAddContainers(V1alpha1Processor processor, Map<String, String> envs)
            throws ApiException {

        List<V1Container> containers = new ArrayList<>();

        // Add Multibinder Container
        try {
            V1Container srProcessorContainer = this.yamlMapper
                    .readValue(SRP_PROCESSOR_CONTAINER_TEMPLATE.getInputStream(),
                            V1Container.class);
            List<V1EnvVar> containerVariables = Optional.ofNullable(srProcessorContainer.getEnv())
                    .orElse(new ArrayList<>());

            containerVariables.addAll(envs.entrySet().stream()
                    .map(e -> new V1EnvVarBuilder()
                            .withName(e.getKey())
                            .withValue(e.getValue())
                            .build())
                    .collect(Collectors.toList()));

            this.configureRemoteDebuggingIfEnabled(processor, srProcessorContainer, containerVariables);

            srProcessorContainer.setEnv(containerVariables);

            containers.add(srProcessorContainer);

        }
        catch (Exception e) {
            throw new ApiException(e);
        }

        // Add Side Cars if Defined
        if (processor.getSpec().getTemplate() != null) {
            for (V1alpha1ProcessorSpecTemplateSpecContainers udfContainerDef : processor.getSpec().getTemplate()
                    .getSpec()
                    .getContainers()) {

                V1Container udfContainer = new V1ContainerBuilder()
                        .withName(udfContainerDef.getName())
                        .withImage(udfContainerDef.getImage())
                        .withEnv(Optional.ofNullable(udfContainerDef.getEnv()).orElse(new ArrayList<>())
                                .stream()
                                .map(env -> new V1EnvVarBuilder()
                                        .withName(env.getName())
                                        .withValue(env.getValue())
                                        .build())
                                .collect(Collectors.toList()))
                        .build();

                containers.add(udfContainer);
            }
        }
        return containers;
    }
}
