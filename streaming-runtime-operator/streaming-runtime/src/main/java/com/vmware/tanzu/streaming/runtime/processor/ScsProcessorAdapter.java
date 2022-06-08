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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import com.vmware.tanzu.streaming.models.V1alpha1ProcessorSpecTemplateSpecContainers;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarBuilder;
import io.kubernetes.client.openapi.models.V1HTTPGetActionBuilder;
import io.kubernetes.client.openapi.models.V1ProbeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class ScsProcessorAdapter extends AbstractScsProcessorAdapter {

        private static final Logger LOG = LoggerFactory.getLogger(ScsProcessorAdapter.class);
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        public ScsProcessorAdapter(ObjectMapper yamlMapper, AppsV1Api appsV1Api, CoreV1Api coreV1Api) {
                super(yamlMapper, appsV1Api, coreV1Api);
        }

        @Override
        public String type() {
                return "SCS";
        }

        @Override
        protected List<V1Container> doAddContainers(V1alpha1Processor processor, Map<String, String> envs) {

                List<V1Container> containers = new ArrayList<>();

                for (V1alpha1ProcessorSpecTemplateSpecContainers procContainer : processor.getSpec().getTemplate()
                                .getSpec().getContainers()) {

                        V1Container container = new V1ContainerBuilder()
                                        .withName(procContainer.getName())
                                        .withImage(procContainer.getImage())
                                        .withEnv(Optional.ofNullable(procContainer.getEnv()).orElse(new ArrayList<>())
                                                        .stream()
                                                        .map(e -> new V1EnvVarBuilder()
                                                                        .withName(e.getName())
                                                                        .withValue(e.getValue())
                                                                        .build())
                                                        .collect(Collectors.toList()))
                                        .withLivenessProbe(new V1ProbeBuilder()
                                                        .withHttpGet(new V1HTTPGetActionBuilder()
                                                                        .withPath("/actuator/health")
                                                                        .withPort(new IntOrString(8080))
                                                                        .build())
                                                        .withInitialDelaySeconds(10)
                                                        .withPeriodSeconds(60)
                                                        .build())
                                        .withReadinessProbe(new V1ProbeBuilder()
                                                        .withHttpGet(new V1HTTPGetActionBuilder()
                                                                        .withPath("/actuator/info")
                                                                        .withPort(new IntOrString(8080))
                                                                        .build())
                                                        .withInitialDelaySeconds(10)
                                                        .withPeriodSeconds(10)
                                                        .build())
                                        .build();

                        List<V1EnvVar> containerVariables = Optional.ofNullable(container.getEnv())
                                        .orElse(new ArrayList<>());

                        containerVariables.addAll(envs.entrySet().stream()
                                        .map(e -> new V1EnvVarBuilder()
                                                        .withName(e.getKey())
                                                        .withValue(e.getValue())
                                                        .build())
                                        .collect(Collectors.toList()));

                        container.setEnv(containerVariables);

                        containers.add(container);
                }

                return containers;
        }
}
