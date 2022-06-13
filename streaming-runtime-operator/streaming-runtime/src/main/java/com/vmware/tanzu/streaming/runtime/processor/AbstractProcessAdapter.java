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

import java.util.List;

import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPortBuilder;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarBuilder;

import org.springframework.util.StringUtils;

public abstract class AbstractProcessAdapter implements ProcessorAdapter {

    public static String REMOTE_DEBUG_PROT_ATTRIBUTE_NAME = "remoteDebugPort";

    /**
     * If you set Processor#attribute remoteDebugPort=xyz, then a remote debug configuration will be enabled.
     * 
     * Note: Also you should enable the remote streaming-runtime-operator debugging as well and add brake point inside
     * the ProcessorReconciler#reconcile method. Otherwise the controller will keep re-creating the Processor!
     */
    protected void configureRemoteDebuggingIfEnabled(V1alpha1Processor processor, V1Container container,
            List<V1EnvVar> containerVariables) {
        String remoteDebugPort = this.getRemoteDebugPort(processor);
        if (StringUtils.hasText(remoteDebugPort)) {
            this.configureRemoteDebugging(remoteDebugPort, container, containerVariables);
        }

    }

    private String getRemoteDebugPort(V1alpha1Processor processor) {
        if (processor.getSpec().getAttributes() != null
                && processor.getSpec().getAttributes().containsKey(REMOTE_DEBUG_PROT_ATTRIBUTE_NAME)) {
            return processor.getSpec().getAttributes().get(REMOTE_DEBUG_PROT_ATTRIBUTE_NAME);
        }
        return null;
    }

    private void configureRemoteDebugging(String remoteDebugPort, V1Container scsProcessorContainer,
            List<V1EnvVar> containerVariables) {

        if (StringUtils.hasText(remoteDebugPort)) {
            scsProcessorContainer.getPorts().add(new V1ContainerPortBuilder()
                    .withContainerPort(Integer.valueOf(remoteDebugPort))
                    .withName("remote-debug")
                    .withProtocol("TCP")
                    .build());
            containerVariables.add(
                    new V1EnvVarBuilder()
                            .withName("JAVA_TOOL_OPTIONS")
                            .withValue(String.format(
                                    "-agentlib:jdwp=transport=dt_socket,server=y,address=%s,suspend=y,quiet=y",
                                    remoteDebugPort))
                            .build());
        }
    }

}
