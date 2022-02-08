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
