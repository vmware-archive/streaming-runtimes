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
		return new org.apache.avro.Schema.Parser().parse(inlineAvroSchema);
	}
}
