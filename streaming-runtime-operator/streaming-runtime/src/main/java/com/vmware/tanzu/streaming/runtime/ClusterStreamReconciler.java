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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vmware.tanzu.streaming.apis.StreamingTanzuVmwareComV1alpha1Api;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStream;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamSpecStorageServer;
import com.vmware.tanzu.streaming.runtime.config.ClusterStreamConfiguration;
import com.vmware.tanzu.streaming.runtime.protocol.ProtocolDeploymentAdapter;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.util.PatchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class ClusterStreamReconciler implements Reconciler {

	private static final Logger LOG = LoggerFactory.getLogger(ClusterStreamReconciler.class);
	private static final boolean REQUEUE = true;

	private final Lister<V1alpha1ClusterStream> clusterStreamLister;
	private final Map<String, ProtocolDeploymentAdapter> protocolDeploymentEditors;
	private final EventRecorder eventRecorder;
	private final StreamingTanzuVmwareComV1alpha1Api api;

	public ClusterStreamReconciler(SharedIndexInformer<V1alpha1ClusterStream> clusterStreamInformer,
			ProtocolDeploymentAdapter[] protocolDeploymentEditors, StreamingTanzuVmwareComV1alpha1Api api,
			EventRecorder eventRecorder) {

		this.api = api;
		this.clusterStreamLister = new Lister<>(clusterStreamInformer.getIndexer());
		this.protocolDeploymentEditors = Stream.of(protocolDeploymentEditors)
				.collect(Collectors.toMap(ProtocolDeploymentAdapter::getProtocolDeploymentEditorName,
						Function.identity()));
		this.eventRecorder = eventRecorder;
	}

	@Override
	public Result reconcile(Request request) {

		V1alpha1ClusterStream clusterStream = this.clusterStreamLister.get(request.getName());

		if (clusterStream == null) {
			LOG.error(String.format("Missing ClusterStream: %s", request.getName()));
			return new Result(!REQUEUE);
		}

		String namespace = retrieveNamespace(request, clusterStream);

		try {

			final boolean toDelete = clusterStream.getMetadata().getDeletionTimestamp() != null;

			V1OwnerReference ownerReference = toOwnerReference(clusterStream);

			String serviceBinding = null;

			if (!toDelete) {
				V1alpha1ClusterStreamSpecStorageServer server = clusterStream.getSpec().getStorage().getServer();

				serviceBinding = server.getBinding();

				String protocolAdapterName = this.getProtocolAdapterName(clusterStream.getSpec().getStorage()
						.getAttributes(), server.getProtocol());
				ProtocolDeploymentAdapter protocolDeploymentEditor = this.protocolDeploymentEditors
						.get(protocolAdapterName);
				protocolDeploymentEditor.createIfNotFound(ownerReference, namespace, clusterStream);
				protocolDeploymentEditor.postCreateConfiguration(ownerReference, namespace, clusterStream);
			}

			// Status Update
			boolean isServiceBindingEnabled = StringUtils.hasText(serviceBinding);

			V1alpha1ClusterStreamSpecStorageServer server = clusterStream.getSpec().getStorage().getServer();

			ProtocolDeploymentAdapter protocolAdapter = this.protocolDeploymentEditors.get(
					getProtocolAdapterName(clusterStream.getSpec().getStorage()
							.getAttributes(), server.getProtocol()));

			String storageAddress = "";
			String readyStatus = "false";
			String reason = "ProtocolDeployment";
			if (protocolAdapter.isRunning(ownerReference, namespace)) {
				storageAddress = protocolAdapter.getStorageAddress(ownerReference, namespace, isServiceBindingEnabled);
				if (StringUtils.hasText(storageAddress)) {
					readyStatus = "true";
					reason = "ProtocolDeployed";
				}
			}

			boolean isStatusReady = StringUtils.hasText(storageAddress);

			this.setClusterStreamStatus(clusterStream, readyStatus, reason, storageAddress, serviceBinding);

			if (!isStatusReady) {
				return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
			}
		}
		catch (ApiException e) {
			if (e.getCode() == 409) {
				LOG.info("Required subresource is already present, skip creation.");
				return new Result(!REQUEUE);
			}
			logFailureEvent(clusterStream, namespace, e.getCode() + " - " + e.getResponseBody(), e);
			return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
		}
		catch (Exception e) {
			logFailureEvent(clusterStream, namespace, e.getMessage(), e);
			return new Result(REQUEUE, Duration.of(15, ChronoUnit.SECONDS));
		}
		return new Result(!REQUEUE);
	}

	private String retrieveNamespace(Request request, V1alpha1ClusterStream clusterStream) {

		String namespace = request.getNamespace();

		Map<String, String> attributes = clusterStream.getSpec().getStorage().getAttributes();
		if (!StringUtils.hasText(namespace) && !CollectionUtils.isEmpty(attributes)
				&& StringUtils.hasText(attributes.get("namespace"))) {
			namespace = attributes.get("namespace");
		}
		return StringUtils.hasText(namespace) ? namespace : "default";
	}

	private String getProtocolAdapterName(Map<String, String> attributes, String protocolName) {
		String protocolAdapterName = (attributes != null) ? attributes.get("protocolAdapterName") : "";
		if (!StringUtils.hasText(protocolAdapterName)) {
			protocolAdapterName = protocolName;
		}
		return protocolAdapterName;
	}

	private V1OwnerReference toOwnerReference(V1alpha1ClusterStream clusterStream) {
		return new V1OwnerReference().controller(true)
				.name(clusterStream.getMetadata().getName())
				.uid(clusterStream.getMetadata().getUid())
				.kind(clusterStream.getKind())
				.apiVersion(clusterStream.getApiVersion())
				.blockOwnerDeletion(true);
	}

	private void logFailureEvent(V1alpha1ClusterStream clusterStream, String namespace, String reason, Exception e) {
		String message = String.format("Failed to deploy Cluster Stream %s: %s", clusterStream.getMetadata()
				.getName(), reason);
		LOG.error(message, e);
		eventRecorder.logEvent(
				EventRecorder.toObjectReference(clusterStream).namespace(namespace),
				null,
				ClusterStreamConfiguration.CLUSTER_STREAM_CONTROLLER_NAME,
				e.getClass().getName(),
				message + ": " + e.getMessage(),
				EventType.Warning);
	}

	private void setClusterStreamStatus(V1alpha1ClusterStream clusterStream, String status, String reason,
			String serverAddresses, String binding) {

		String bindingStatus = (StringUtils.hasText(binding))
				? String.format("	\"binding\": {\"name\": \"%s\"}, ", binding)
				: "";

		if (hasConditionChanged(clusterStream, status, reason)) {
			String patch = String.format("" +
					"{\"status\": " +
					"  {%s " +
					"	\"conditions\": " +
					"      [{ \"type\": \"%s\", " +
					"         \"status\": \"%s\", " +
					"         \"lastTransitionTime\": \"%s\", " +
					"         \"reason\": \"%s\"}]," +
					"   \"storageAddress\": { \"server\": { %s } }" +
					"  }" +
					"}",
					bindingStatus, "Ready", status, ZonedDateTime.now(ZoneOffset.UTC), reason, serverAddresses);
			try {
				PatchUtils.patch(V1alpha1ClusterStream.class,
						() -> api.patchClusterStreamStatusCall(
								clusterStream.getMetadata().getName(),
								new V1Patch(patch), null, null, null, null),
						V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
						api.getApiClient());
			}
			catch (ApiException e) {
				LOG.error("Status API call failed: {}: {}, {}, with patch {}", e.getCode(), e.getMessage(),
						e.getResponseBody(), patch);
			}
		}
	}

	private boolean hasConditionChanged(V1alpha1ClusterStream clusterStream, String newReadyStatus,
			String newStatusReason) {
		if (clusterStream.getStatus() == null || clusterStream.getStatus().getConditions() == null) {
			return true;
		}

		return !clusterStream.getStatus().getConditions().stream().allMatch(
				condition -> newReadyStatus.equalsIgnoreCase(condition.getStatus())
						&& newStatusReason.equalsIgnoreCase(condition.getReason()));
	}

}
