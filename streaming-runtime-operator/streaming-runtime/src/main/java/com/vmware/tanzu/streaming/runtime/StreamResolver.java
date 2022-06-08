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
package com.vmware.tanzu.streaming.runtime;

import com.vmware.tanzu.streaming.apis.StreamingTanzuVmwareComV1alpha1Api;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.models.V1alpha1StreamList;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class StreamResolver {

    private static final Logger LOG = LoggerFactory.getLogger(StreamResolver.class);

    public static final String TRUE = "true";
	public static final String FALSE = "false";
    public static final String READY_STATUS_TYPE = "Ready";
    
    private final StreamingTanzuVmwareComV1alpha1Api api;
    
    public StreamResolver(StreamingTanzuVmwareComV1alpha1Api api) {
        this.api = api;
    }

    public V1alpha1Stream getStreamByName(String streamName) throws ApiException, ProcessorStatusException {

		V1alpha1StreamList streamList = this.api.listStreamForAllNamespaces(null, null,
				"metadata.name=" + streamName, null, null,
				null, null, null, null, null);

		if (CollectionUtils.isEmpty(streamList.getItems())) {
			// this.setProcessorStatus(processor, FALSE, "ProcessorMissingStream");
			// throw new ApiException("Missing Stream: " + streamName);
			throw new ProcessorStatusException(null, "FALSE", "ProcessorMissingStream", "Missing Stream: " + streamName);
		}

		if (streamList.getItems().size() > 1) {
			LOG.warn(String.format("Many (%s) Streams with name: %s found! Only the first is used!",
					streamList.getItems().size(), streamName));
		}

		V1alpha1Stream stream = streamList.getItems().get(0);

		if (stream == null) {
			// this.setProcessorStatus(processor, FALSE, "ProcessorMissingStream");
			// throw new ApiException("MissingStream: " + streamName);
			throw new ProcessorStatusException(null, FALSE, "ProcessorMissingStream", "MissingStream: " + streamName);
		}
		if (!isStreamReady(stream)) {
			// this.setProcessorStatus(processor, FALSE, "ProcessorStreamNotReady");
			// throw new ApiException("StreamNotReady: " + streamName);
			throw new ProcessorStatusException(null, FALSE, "ProcessorStreamNotReady", "StreamNotReady: " + streamName);
		}

		return stream;
	}

    private boolean isStreamReady(V1alpha1Stream stream) {
		if (stream.getStatus() == null || stream.getStatus().getConditions() == null) {
			return false;
		}

		return stream.getStatus().getConditions().stream()
				.filter(c -> READY_STATUS_TYPE.equalsIgnoreCase(c.getType()))
				.allMatch(c -> TRUE.equalsIgnoreCase(c.getStatus()));
	}

}
