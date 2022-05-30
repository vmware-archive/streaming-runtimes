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

import org.apache.avro.Schema;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class InlineAvroToAvroConverter implements DataSchemaAvroConverter {

	public static final String TYPE = "avro";

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

		String inlineAvroSchema = context.getDataSchemaContext().getInline().getSchema();
		String jsonAvroSchema = AvroHelper.convertYamlOrJsonToJson(inlineAvroSchema);
		return new org.apache.avro.Schema.Parser().parse(jsonAvroSchema);
	}
}
