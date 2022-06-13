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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.streaming.apis.StreamingTanzuVmwareComV1alpha1Api;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStream;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamList;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamSpec;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamSpecStorage;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamSpecStorageServer;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamStatusConditions;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.runtime.config.StreamConfiguration;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.PatchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class StreamReconciler implements Reconciler {

	private static final Logger LOG = LoggerFactory.getLogger(StreamReconciler.class);
	private static final boolean REQUEUE = true;
	private static final String FINALIZER_STRING = "finalizer.streams.streaming.tanzu.vmware.com";
	private static final String DEFAULT_CLUSTER_STREAM_SUFFIX = "-cluster-stream";

	private final Lister<V1alpha1Stream> streamLister;
	private final EventRecorder eventRecorder;
	private final StreamingTanzuVmwareComV1alpha1Api api;
	private final StreamingRuntimeProperties streamingRuntimeProperties;

	public StreamReconciler(SharedIndexInformer<V1alpha1Stream> streamInformer,
			StreamingTanzuVmwareComV1alpha1Api api, EventRecorder eventRecorder,
			StreamingRuntimeProperties streamingRuntimeProperties) {
		this.api = api;
		this.streamingRuntimeProperties = streamingRuntimeProperties;
		this.streamLister = new Lister<>(streamInformer.getIndexer());
		this.eventRecorder = eventRecorder;
	}

	@Override
	public Result reconcile(Request request) {

		String streamName = request.getName();
		String streamNamespace = request.getNamespace();

		V1alpha1Stream stream = this.streamLister.namespace(streamNamespace).get(streamName);

		if (stream == null) {
			LOG.error(String.format("Missing Stream: %s/%s", streamNamespace, streamName));
			return new Result(!REQUEUE);
		}

		try {

			final boolean toDelete = stream.getMetadata().getDeletionTimestamp() != null;

			String clusterStreamName = this.getClusterStreamName(stream, streamName);

			if (toDelete) {
				removeFinalizer(stream);
			}
			else {

				V1alpha1ClusterStream clusterStream = findClusterStream(clusterStreamName);

				if (clusterStream == null) {
					if (this.streamingRuntimeProperties.isAutoProvisionClusterStream()) {
						this.autoProvisionCusterStream(clusterStreamName, streamNamespace, stream);
						this.setStreamStatus(stream, "false", "AutoProvisionClusterStream", null);
						throw new ApiException(String.format("Auto-provision ClusterStream: %s for Stream: %s",
								clusterStreamName, streamName));
					}
					else {
						this.setStreamStatus(stream, "false", "NoClusterStreamFound", null);
						throw new ApiException(String.format("No ClusterStream: %s found for Stream: %s",
								clusterStreamName, streamName));
					}
				}

				if (clusterStream.getStatus() == null
						|| clusterStream.getStatus().getConditions() == null
						|| !clusterStream.getStatus().getConditions().stream()
								.map(V1alpha1ClusterStreamStatusConditions::getStatus)
								.allMatch("true"::equalsIgnoreCase)) {

					this.setStreamStatus(stream, "false", "ClusterStreamNotReady", null);
					throw new ApiException(
							String.format("Not Ready ClusterStream: %s for Stream: %s", clusterStreamName, streamName));
				}

				// Validate that the Stream and ClusterStream protocols match!
				if (clusterStream.getStatus().getStorageAddress() == null
						|| clusterStream.getStatus().getStorageAddress().getServer() == null
						|| !clusterStream.getStatus().getStorageAddress().getServer().values().stream()
								.allMatch(s -> s.getProtocol() != null ? s.getProtocol()
										.equalsIgnoreCase(stream.getSpec().getProtocol()) : false)) {
					setStreamStatus(stream, "false", "ProtocolMismatch", null);
					throw new ApiException(
							String.format("Stream (%s) protocol (%s) doesn't match the ClusterStream: %s",
									streamName, stream.getSpec().getProtocol(), clusterStreamName));
				}

				addFinalizerIfNotFound(stream);

				String storageAddress = new ObjectMapper()
						.writeValueAsString(clusterStream.getStatus().getStorageAddress());
				boolean isStatusReady = StringUtils.hasText(storageAddress);
				String readyStatus = isStatusReady ? "true" : "false";

				String statusReason = isStatusReady ? "StreamDeployed" : "DeployingStream";

				setStreamStatus(stream, readyStatus, statusReason, "\"storageAddress\": " + storageAddress);

				if (!isStatusReady) {
					return new Result(REQUEUE, Duration.of(30, ChronoUnit.SECONDS));
				}
			}
		}
		catch (ApiException e) {
			if (e.getCode() == 409) {
				LOG.info("Required subresource is already present, skip creation.");
				return new Result(!REQUEUE);
			}
			logFailureEvent(stream, e.getMessage(), e.getCode() + " - " + e.getResponseBody(), e);

			return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
		}
		catch (Exception e) {
			logFailureEvent(stream, e.getMessage(), "", e);
			return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
		}
		return new Result(!REQUEUE);
	}

	private String getClusterStreamName(V1alpha1Stream stream, String streamName) {

		String clusterStreamName = null;
		if (stream.getSpec().getStorage() != null && stream.getSpec().getStorage().getClusterStream() != null) {
			clusterStreamName = stream.getSpec().getStorage().getClusterStream();
		}

		clusterStreamName = StringUtils.hasText(clusterStreamName) ? clusterStreamName
				: streamName + DEFAULT_CLUSTER_STREAM_SUFFIX;

		return clusterStreamName;
	}

	private boolean hasConditionChanged(V1alpha1Stream stream, String newReadyStatus, String newStatusReason) {
		if (stream.getStatus() == null || stream.getStatus().getConditions() == null) {
			return true;
		}

		return !stream.getStatus().getConditions().stream().allMatch(
				condition -> newReadyStatus.equalsIgnoreCase(condition.getStatus())
						&& newStatusReason.equalsIgnoreCase(condition.getReason()));
	}

	private V1alpha1ClusterStream findClusterStream(String clusterStreamName) throws ApiException {

		V1alpha1ClusterStreamList clusterStreamList = api.listClusterStream(
				null, null, null,
				"metadata.name=" + clusterStreamName, null, null,
				null, null, null, false);

		// should only be one?
		return clusterStreamList.getItems().size() > 0 ? clusterStreamList.getItems().get(0) : null;
	}

	private void logFailureEvent(V1alpha1Stream stream, String reason, String errorBody, Exception e) {
		String message = String.format("Failed to %s for Stream %s/%s: %s",
				reason, stream.getMetadata().getNamespace(), stream.getMetadata().getName(), errorBody);
		LOG.warn(message);
		eventRecorder.logEvent(
				EventRecorder.toObjectReference(stream),
				null,
				StreamConfiguration.STREAM_CONTROLLER_NAME,
				e.getClass().getName(),
				message + ": " + e.getMessage(),
				EventType.Warning);
	}

	private void setStreamStatus(V1alpha1Stream stream, String status, String reason,
			String storageAddress) {

		if (!hasConditionChanged(stream, status, reason)) {
			return;
		}

		if (StringUtils.hasText(storageAddress)) {
			storageAddress = "," + storageAddress;
		}
		else {
			storageAddress = "";
		}

		String patch = String.format("" +
				"{\"status\": " +
				"  {\"conditions\": " +
				"      [{ \"type\": \"%s\", \"status\": \"%s\", \"lastTransitionTime\": \"%s\", \"reason\": \"%s\"}]" +
				"     %s" +
				"  }" +
				"}",
				"Ready", status, ZonedDateTime.now(ZoneOffset.UTC), reason, storageAddress);
		try {
			PatchUtils.patch(
					V1alpha1Stream.class,
					() -> api.patchNamespacedStreamStatusCall(
							stream.getMetadata().getName(),
							stream.getMetadata().getNamespace(),
							new V1Patch(patch),
							null, null, null, null),
					V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
					api.getApiClient());

		}
		catch (ApiException e) {
			LOG.error("Status API call failed: {}: {}, {}, with patch {}", e.getCode(), e.getMessage(),
					e.getResponseBody(), patch);
		}
	}

	private void addFinalizerIfNotFound(V1alpha1Stream stream) throws ApiException {
		LOG.debug("Checking for existing finalizers");
		boolean notFound = stream.getMetadata().getFinalizers() == null || stream.getMetadata().getFinalizers()
				.isEmpty();
		if (notFound) {
			LOG.debug("Finalizers not found, adding one");
			streamPatch(stream, "{\"metadata\":{\"finalizers\":[\"" + FINALIZER_STRING + "\"]}}",
					V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH);
		}
	}

	private V1alpha1Stream removeFinalizer(V1alpha1Stream stream) throws ApiException {
		// Currently, we don't have other finalizers so for now we just recklessly remove all finalizers.
		LOG.info("Try to remove Finalizers for stream: " + stream.getMetadata().getName());
		try {
			return streamPatch(stream, "[{\"op\": \"remove\", \"path\": \"/metadata/finalizers\"}]",
					V1Patch.PATCH_FORMAT_JSON_PATCH);
		}
		catch (ApiException e) {
			LOG.error("Finalizer removal problem", e);
			throw e;
		}
	}

	// NOTE: The api.patchNamespacedStreamCall(...) won't patch Stream's status! For this use the
	// api.patchNamespacedStreamStatusCall(...) install
	private V1alpha1Stream streamPatch(V1alpha1Stream stream, String jsonPatch, String patchFormat)
			throws ApiException {
		return PatchUtils.patch(
				V1alpha1Stream.class,
				() -> api.patchNamespacedStreamCall(
						stream.getMetadata().getName(),
						stream.getMetadata().getNamespace(),
						new V1Patch(jsonPatch),
						null, null, null, null),
				patchFormat,
				api.getApiClient());
	}

	private void autoProvisionCusterStream(String clusterStreamName, String streamNamespace, V1alpha1Stream stream)
			throws ApiException {

		if (this.findClusterStream(clusterStreamName) != null) {
			return;
		}

		V1alpha1ClusterStream cs = new V1alpha1ClusterStream();
		cs.setApiVersion("streaming.tanzu.vmware.com/v1alpha1");
		cs.setKind("ClusterStream");

		cs.setMetadata(new V1ObjectMeta());
		cs.getMetadata().setName(clusterStreamName);
		cs.getMetadata().setLabels(new HashMap<>());
		cs.getMetadata().getLabels().put("name", clusterStreamName);
		cs.setSpec(new V1alpha1ClusterStreamSpec());
		cs.getSpec().setName(stream.getSpec().getName());
		if (!CollectionUtils.isEmpty(stream.getSpec().getKeys())) {
			cs.getSpec().setKeys(stream.getSpec().getKeys());
		}
		else {
			cs.getSpec().setKeys(new ArrayList<>());
		}

		cs.getSpec().setStreamModes((CollectionUtils.isEmpty(stream.getSpec().getStreamMode()) ? List.of("read")
				: stream.getSpec().getStreamMode()));

		cs.getSpec().setStorage(new V1alpha1ClusterStreamSpecStorage());
		cs.getSpec().getStorage().setAttributes(new HashMap<>());
		cs.getSpec().getStorage().getAttributes().put("namespace", streamNamespace);

		String protocolAdapterName = CollectionUtils.isEmpty(stream.getSpec().getAttributes()) ? ""
				: stream.getSpec().getAttributes().get("protocolAdapterName");
		if (StringUtils.hasText(protocolAdapterName)) {
			cs.getSpec().getStorage().getAttributes().put("protocolAdapterName", protocolAdapterName);
		}
		cs.getSpec().getStorage().setReclaimPolicy("Retain");

		V1alpha1ClusterStreamSpecStorageServer server = new V1alpha1ClusterStreamSpecStorageServer();
		server.setProtocol(stream.getSpec().getProtocol());
		server.setUrl("empty");
		if (StringUtils.hasText(stream.getSpec().getBinding())) {
			server.setBinding(stream.getSpec().getBinding());
		}
		cs.getSpec().getStorage().setServer(server);

		api.createClusterStream(cs, null, null, null);
	}
}
