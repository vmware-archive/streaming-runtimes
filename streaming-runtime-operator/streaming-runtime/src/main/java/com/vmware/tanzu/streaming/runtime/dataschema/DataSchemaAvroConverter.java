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
import org.apache.avro.SchemaBuilder;

/**
 * Implementers of this interface can convert some custom Data Schema representation into a common Avro schema.
 */
public interface DataSchemaAvroConverter {

	/**
	 * @return Returns the data schema representation type that this convertor support.
	 */
	String getSupportedDataSchemaType();

	/**
	 * Computes an Avro schema instance form the provided context.
	 *
	 * @param context All stream schema information necessary to compute the Avro schema. The context is mutable,
	 *                   allowing converters to alter its state!
	 *
	 * @return Returns Avro schema instance computed form the input context. Mind that the context is mutable as well
	 *         allowing the convertor implementation to alter context's internal structures.
	 */
	Schema toAvro(DataSchemaProcessingContext context);

	default Schema nullableSchema(Schema schema) {
		return schema.isNullable() ? schema : Schema.createUnion(SchemaBuilder.builder().nullType(), schema);
	}
}
