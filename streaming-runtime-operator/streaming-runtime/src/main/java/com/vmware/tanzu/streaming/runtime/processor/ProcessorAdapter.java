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
import java.util.List;

import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.runtime.ProcessorStatusException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1OwnerReference;

/**
 * Adapter for different types of streaming (CRD) Processor implementations. Uses the V1alpha1Processor attributes to
 * configure the different processor types.
 */
public interface ProcessorAdapter {

    /**
     * @return Processor adapter type. Currently supported types are: SCW, SCS, FSQL.
     */
    String type();

    /**
     * Instantiate the Processor Adapter. Could be
     * 
     * @param processor Processor CRD definition.
     * @param ownerReference Processor's resource owner.
     * @param inputStreams List of input Streams.
     * @param outputStreams List of output Streams.
     * @throws IOException
     * @throws ApiException
     * @throws ProcessorStatusException
     */
    void createProcessor(V1alpha1Processor processor, V1OwnerReference ownerReference,
            List<V1alpha1Stream> inputStreams, List<V1alpha1Stream> outputStreams)
            throws IOException, ApiException, ProcessorStatusException;
}
