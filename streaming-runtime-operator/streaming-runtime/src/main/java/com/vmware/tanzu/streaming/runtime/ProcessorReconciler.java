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

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.streaming.apis.StreamingTanzuVmwareComV1alpha1Api;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamStatusBinding;
import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.models.V1alpha1StreamList;
import com.vmware.tanzu.streaming.models.V1alpha1StreamSpec;
import com.vmware.tanzu.streaming.models.V1alpha1StreamSpecStorage;
import com.vmware.tanzu.streaming.runtime.config.ProcessorConfiguration;
import com.vmware.tanzu.streaming.runtime.processor.ProcessorAdapter;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.util.PatchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class ProcessorReconciler implements Reconciler {

	private static final String DEFAULT_PROCESSOR_TYPE = "SRP";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessorReconciler.class);

	private static final boolean REQUEUE = true;
	public static final String READY_STATUS_TYPE = "Ready";
	public static final String TRUE = "true";
	public static final String FALSE = "false";

	private final Lister<V1alpha1Processor> processorLister;
	private final CoreV1Api coreV1Api;
	private final EventRecorder eventRecorder;
	private final StreamingTanzuVmwareComV1alpha1Api api;
	private final StreamResolver streamResolver;
	private Map<String, ProcessorAdapter> processorAdapterMapByType = new HashMap<>();

	private final StreamingRuntimeProperties streamingRuntimeProperties;

	public ProcessorReconciler(SharedIndexInformer<V1alpha1Processor> processorInformer,
			StreamingTanzuVmwareComV1alpha1Api api,
			CoreV1Api coreV1Api,
			EventRecorder eventRecorder,
			AppsV1Api appsV1Api,
			ObjectMapper yamlMapper,
			StreamResolver streamResolver,
			ProcessorAdapter[] processorAdapters,
			StreamingRuntimeProperties streamingRuntimeProperties) {

		this.api = api;
		this.streamingRuntimeProperties = streamingRuntimeProperties;
		this.processorLister = new Lister<>(processorInformer.getIndexer());
		this.coreV1Api = coreV1Api;
		this.eventRecorder = eventRecorder;
		this.streamResolver = streamResolver;

		for (ProcessorAdapter processorAdapter : processorAdapters) {
			this.processorAdapterMapByType.put(processorAdapter.type(), processorAdapter);
		}

	}

	@Override
	public Result reconcile(Request request) {

		String processorName = request.getName();
		String processorNamespace = request.getNamespace();

		V1alpha1Processor processor = this.processorLister.namespace(processorNamespace).get(processorName);

		if (processor == null) {
			LOG.error(String.format("Missing Processor: %s/%s", processorNamespace, processorName));
			return new Result(!REQUEUE);
		}

		try {

			final boolean toDelete = processor.getMetadata().getDeletionTimestamp() != null;

			if (toDelete) {
				return new Result(!REQUEUE); // Nothing to do
			}

			List<V1alpha1Stream> inputStreams = this.getValidStreams(processor.getSpec().getInputs(),
					processorNamespace);
			List<V1alpha1Stream> outputStreams = this.getValidStreams(processor.getSpec().getOutputs(),
					processorNamespace);

			// Deploy(or Replace) a processor pods for this processor.
			V1OwnerReference ownerReference = this.toOwnerReference(processor);
			String processorType = (processor.getSpec().getType() == null) ? DEFAULT_PROCESSOR_TYPE : processor.getSpec().getType();
			this.processorAdapterMapByType.get(processorType)
					.createProcessor(processor, ownerReference, inputStreams, outputStreams);

			// Status update
			if (isProcessorPodRunning(processor)) {
				this.setProcessorStatus(processor, TRUE, "ProcessorDeployed");
			}
			else {
				this.setProcessorStatus(processor, FALSE, "ProcessorDeploying");
				return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
			}
		}
		catch (ProcessorStatusException e) {
			this.setProcessorStatus(processor, e.getStatus(), e.getReason());
			logFailureEvent(processor, processorNamespace, e.getMessage(), e);
			return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
		}
		catch (ApiException apiException) {
			String message = apiException.getMessage() + " : \n"
					+ apiException.getResponseBody() + " : \n"
					+ apiException.getResponseHeaders().toString();
			logFailureEvent(processor, processorNamespace, message, apiException);
			return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
		}
		catch (Exception e) {
			logFailureEvent(processor, processorNamespace, e.getMessage(), e);
			return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
		}

		return new Result(!REQUEUE);
	}

	private List<V1alpha1Stream> getValidStreams(
			List<V1alpha1ClusterStreamStatusBinding> streamDefs, String namespace)
			throws ApiException, ProcessorStatusException {

		List<V1alpha1Stream> streams = new ArrayList<>();
		if (streamDefs != null) {
			for (V1alpha1ClusterStreamStatusBinding sd : streamDefs) {
				try {
					V1alpha1Stream stream = this.streamResolver.getStreamByName(sd.getName());
					streams.add(stream);
				}
				catch (ProcessorStatusException pse) {
					if ("ProcessorMissingStream".equalsIgnoreCase(pse.getReason())) {
						if (this.streamingRuntimeProperties.isAutoProvisionStream()) {
							this.autoProvisionStream(sd.getName(), namespace);
							throw new ProcessorStatusException(null, FALSE, "ProcessorStreamNotReady",
									"StreamNotReady: " + sd.getName());
						}
					}

					throw pse;
				}
			}
		}
		return streams;

	}

	private void autoProvisionStream(String streamName, String namespace) throws ApiException {

		V1alpha1StreamList streamList = this.api.listNamespacedStream(namespace, null, null, null,
				"metadata.name=" + streamName, null, null, null, null, null, null);

		if (streamList.getItems().size() > 0 && streamList.getItems().get(0) != null) {
			return;
		}

		V1alpha1Stream stream = new V1alpha1Stream();
		stream.setApiVersion("streaming.tanzu.vmware.com/v1alpha1");
		stream.setKind("Stream");

		stream.setMetadata(new V1ObjectMeta());
		stream.getMetadata().setName(streamName);
		stream.getMetadata().setLabels(new HashMap<>());
		stream.getMetadata().getLabels().put("name", streamName);

		stream.setSpec(new V1alpha1StreamSpec());
		stream.getSpec().setName(streamName);
		stream.getSpec().setProtocol("kafka"); // TODO

		stream.getSpec().setStorage(new V1alpha1StreamSpecStorage());
		stream.getSpec().getStorage().setClusterStream(streamName + "-cluster-stream");

		this.api.createNamespacedStream(namespace, stream, null, null, null);
	}

	public boolean isProcessorPodExists(V1alpha1Processor processor) {
		try {
			int replicas = (processor.getSpec().getReplicas() != null) ? processor.getSpec().getReplicas() : 1;
			return this.coreV1Api.listNamespacedPod(processor.getMetadata().getNamespace(), null, null, null,
					null,
					"app in (streaming-runtime-processor),streaming-runtime=" + processor.getMetadata().getName(),
					null, null, null, null, null).getItems().size() == replicas;
		}
		catch (ApiException e) {
			LOG.warn("Failed to check the processor Pod existence", e);
		}
		return false;
	}

	private boolean isProcessorPodRunning(V1alpha1Processor processor) {
		try {
			int replicas = (processor.getSpec().getReplicas() != null) ? processor.getSpec().getReplicas() : 1;
			return this.coreV1Api.listNamespacedPod(processor.getMetadata().getNamespace(), null, null, null,
					"status.phase=Running",
					"app in (streaming-runtime-processor),streaming-runtime=" + processor.getMetadata().getName(),
					null, null, null, null, null).getItems().size() == replicas;
		}
		catch (ApiException e) {
			LOG.warn("Failed to check if the processor Pod running", e);
		}
		return false;
	}

	private V1OwnerReference toOwnerReference(V1alpha1Processor processor) {
		return new V1OwnerReference().controller(true)
				.name(processor.getMetadata().getName())
				.uid(processor.getMetadata().getUid())
				.kind(processor.getKind())
				.apiVersion(processor.getApiVersion())
				.blockOwnerDeletion(true);
	}

	private void logFailureEvent(V1alpha1Processor processor, String namespace, String reason, Exception e) {
		String message = String.format("Failed to deploy Processor %s: %s", processor.getMetadata().getName(), reason);
		LOG.warn(message, e);
		this.eventRecorder.logEvent(
				EventRecorder.toObjectReference(processor).namespace(namespace),
				null,
				ProcessorConfiguration.PROCESSOR_CONTROLLER_NAME,
				e.getClass().getName(),
				message + ": " + e.getMessage(),
				EventType.Warning);
	}

	private void setProcessorStatus(V1alpha1Processor processor, String status, String reason) {

		if (!hasProcessorConditionChanged(processor, status, reason)) {
			return;
		}

		String patch = String.format(""
				+ "{"
				+ " \"status\": {"
				+ "   \"conditions\": [{ "
				+ "     \"type\": \"%s\", "
				+ "     \"status\": \"%s\", "
				+ "     \"lastTransitionTime\": \"%s\", "
				+ "     \"reason\": \"%s\""
				+ "    }]"
				+ "  }"
				+ "}",
				READY_STATUS_TYPE, status, ZonedDateTime.now(ZoneOffset.UTC), reason);

		try {
			PatchUtils.patch(V1alpha1Processor.class,
					() -> this.api.patchNamespacedProcessorStatusCall(
							processor.getMetadata().getName(),
							processor.getMetadata().getNamespace(),
							new V1Patch(patch),
							null, null, null, null),
					V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
					this.api.getApiClient());
		}
		catch (ApiException apiException) {
			LOG.error("Status API call failed: {}: {}, {}, with patch {}",
					apiException.getCode(), apiException.getMessage(), apiException.getResponseBody(), patch);
		}
	}

	private boolean hasProcessorConditionChanged(
			V1alpha1Processor processor, String newReadyStatus, String newStatusReason) {

		if (processor.getStatus() == null || processor.getStatus().getConditions() == null) {
			return true;
		}

		return !processor.getStatus().getConditions().stream()
				.filter(condition -> READY_STATUS_TYPE.equalsIgnoreCase(condition.getType()))
				.allMatch(condition -> newReadyStatus.equalsIgnoreCase(condition.getStatus())
						&& newStatusReason.equalsIgnoreCase(condition.getReason()));
	}
}
