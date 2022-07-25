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
package com.vmware.tanzu.streaming.runtime.protocol;

import com.vmware.tanzu.streaming.models.V1alpha1ClusterStream;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1OwnerReference;

public interface ProtocolDeploymentAdapter {

	/**
	 * @return Identifier of the binder protocol supported by this deployment editor.
	 */
	String getProtocolName();

	/**
	 * Identifies uniquely the protocol deployment editor.
	 * It defaults to the protocolName.
	 * @return Unique protocol deployment editor identifier.
	 */
	default String getProtocolDeploymentEditorName() {
		return getProtocolName();
	}

	void createIfNotFound(V1OwnerReference ownerReference, String namespace, V1alpha1ClusterStream clusterStream) throws ApiException;

	default void postCreateConfiguration(V1OwnerReference ownerReference, String namespace,
			V1alpha1ClusterStream clusterStream) throws ApiException {
	}

	boolean isRunning(V1OwnerReference ownerReference, String namespace);

	String getStorageAddress(V1OwnerReference ownerReference, String namespace, boolean isServiceBindingEnabled);
}
