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
package com.vmware.tanzu.streaming.runtime.dataschema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamStatusStorageAddressServer;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.models.V1alpha1StreamSpecDataSchemaContext;
import com.vmware.tanzu.streaming.models.V1alpha1StreamSpecDataSchemaContextMetadataFields;
import com.vmware.tanzu.streaming.models.V1alpha1StreamSpecDataSchemaContextTimeAttributes;
import io.kubernetes.client.openapi.ApiException;

import org.springframework.util.StringUtils;

public class DataSchemaProcessingContext {

	public static final String META_SCHEMA_TYPE = "meta-schema";
	public static final String STREAM_STATUS_SERVER_PREFIX = "stream.status.server.";

	private final String streamName;
	private final String streamProtocol;
	private final V1alpha1StreamSpecDataSchemaContext dataSchemaContext;
	private final java.util.Map<String, V1alpha1StreamSpecDataSchemaContextMetadataFields> metadataFields;
	private final Map<String, String> timeAttributes;
	private final Map<String, String> options;

	private DataSchemaProcessingContext(String streamName, String streamProtocol,
			V1alpha1StreamSpecDataSchemaContext streamDataSchema,
			Map<String, V1alpha1StreamSpecDataSchemaContextMetadataFields> metadataFields,
			Map<String, String> timeAttributes,
			Map<String, String> options) {
		this.streamName = streamName;
		this.streamProtocol = streamProtocol;
		this.dataSchemaContext = streamDataSchema;
		this.metadataFields = metadataFields;
		this.timeAttributes = timeAttributes;
		this.options = options;
	}

	public static DataSchemaProcessingContext of(V1alpha1Stream stream) throws ApiException {
		V1alpha1StreamSpecDataSchemaContext dataSchemaCtx = stream.getSpec().getDataSchemaContext();
		if (dataSchemaCtx == null) {
			throw new ApiException("Missing dataSchema for: " + stream.getMetadata().getName());
		}

		// Retrieve Stream's metadata fields grouped by field names.
		Map<String, V1alpha1StreamSpecDataSchemaContextMetadataFields> metadataFields =
				Optional.ofNullable(dataSchemaCtx.getMetadataFields()).orElse(new ArrayList<>()).stream()
						.collect(Collectors.toMap(V1alpha1StreamSpecDataSchemaContextMetadataFields::getName, f -> f));

		// Retrieve Stream's time attributes in pairs of field-name -> watermark-value.
		// Empty watermark stands Proctime time attribute!
		Map<String, String> timeAttributes = Optional.ofNullable(dataSchemaCtx.getTimeAttributes())
				.orElse(new ArrayList<>()).stream()
				.collect(Collectors.toMap(
						V1alpha1StreamSpecDataSchemaContextTimeAttributes::getName,
						ta -> Optional.ofNullable(ta.getWatermark()).orElse("")));

		// Options defined in the Stream's CR definition.
		Map<String, String> dataSchemaContextOptions =
				Optional.ofNullable(dataSchemaCtx.getOptions()).orElse(new HashMap<>());

		// Add all Stream status server variables as options with stream.status.server. prefix.
		// Covert the storage address server information into Flink SQL connector WITH section.
		V1alpha1ClusterStreamStatusStorageAddressServer server = stream.getStatus()
				.getStorageAddress().getServer().values().iterator().next();
		Map<String, String> streamStatusServerVariables = server.getVariables();
		streamStatusServerVariables.entrySet().stream().forEach(v -> {
			dataSchemaContextOptions.put(STREAM_STATUS_SERVER_PREFIX + v.getKey(), v.getValue());
		});

		return new DataSchemaProcessingContext(
				stream.getSpec().getName(),
				stream.getSpec().getProtocol(),
				dataSchemaCtx,
				metadataFields,
				timeAttributes,
				dataSchemaContextOptions);
	}

	public V1alpha1StreamSpecDataSchemaContext getDataSchemaContext() {
		return dataSchemaContext;
	}

	public Map<String, V1alpha1StreamSpecDataSchemaContextMetadataFields> getMetadataFields() {
		return metadataFields;
	}

	public Map<String, String> getTimeAttributes() {
		return timeAttributes;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public String getStreamName() {
		return streamName;
	}

	public String getStreamProtocol() {
		return streamProtocol;
	}

	public boolean isDataSchemaAvailable() {
		return StringUtils.hasText(this.getDataSchemaType());
	}

	public String getDataSchemaType() {
		if (this.dataSchemaContext.getInline() != null) {
			return this.dataSchemaContext.getInline().getType();
		}
		else if (dataSchemaContext.getSchema() != null) {
			return META_SCHEMA_TYPE;
		}
		return null;
	}
}
