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

import java.util.Map;

import org.apache.avro.Schema;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class InlineAvroConfluentToAvroConverter implements DataSchemaAvroConverter {

	public static final String TYPE = "avro-confluent";

	private final RestTemplate restTemplate = new RestTemplateBuilder().build();

	@Override
	public String getSupportedDataSchemaType() {
		return TYPE;
	}

	@Override
	public Schema toAvro(DataSchemaProcessingContext context) {

		Assert.isTrue(getSupportedDataSchemaType().equalsIgnoreCase(
						context.getDataSchemaContext().getInline().getType()),
				String.format("Wrong schema representation: %s for converter type %s",
						context.getDataSchemaContext().getInline().getType(), this.getSupportedDataSchemaType()));

		// Schema url pointing to remote schema registry.
		String schemaUrl = context.getDataSchemaContext().getInline().getSchema();

		// If the schema url is empty then try to generate on based on the Topic and the Schema registry URL.
		if (!StringUtils.hasText(schemaUrl) || "default".equalsIgnoreCase(schemaUrl)) {
			String schemaRegistryUrl = context.getOptions().get("stream.status.server.schemaRegistry");
			Assert.hasText(schemaRegistryUrl, "Missing schema registry Url ");
			String streamName = context.getOptions().get("ddl.topic");
			Assert.hasText(streamName, "Missing topic name ");
			schemaUrl = String.format("%s/subjects/%s-value/versions/latest", schemaRegistryUrl, streamName);
		}

		Map<String, ?> subject = this.restTemplate.getForObject(schemaUrl, Map.class);
		String avroSchema = (String) subject.get("schema");

		String jsonAvroSchema = AvroHelper.convertYamlOrJsonToJson(avroSchema);
		return new org.apache.avro.Schema.Parser().parse(jsonAvroSchema);
	}
}
