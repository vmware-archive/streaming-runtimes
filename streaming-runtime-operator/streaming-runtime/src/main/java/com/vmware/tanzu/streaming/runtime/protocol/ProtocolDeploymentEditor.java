package com.vmware.tanzu.streaming.runtime.protocol;

import com.vmware.tanzu.streaming.models.V1alpha1ClusterStream;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1OwnerReference;

public interface ProtocolDeploymentEditor {

	/**
	 * @return Identifier of the binder protocol supported by this deployment editor.
	 */
	String getProtocolName();

	/**
	 * Identifies uniquely the protocol deployment editor.
	 * If not implemented it defaults to the protocolName.
	 * @return Unique protocol deployment editor identifier.
	 */
	default String getProtocolDeploymentEditorName() {
		return getProtocolName();
	}

	void createIfNotFound(V1OwnerReference ownerReference, String namespace) throws ApiException;

	default void postCreateConfiguration(V1OwnerReference ownerReference, String namespace,
			V1alpha1ClusterStream clusterStream) throws ApiException {
	}

	boolean isRunning(V1OwnerReference ownerReference, String namespace);

	String getStorageAddress(V1OwnerReference ownerReference, String namespace);
}
