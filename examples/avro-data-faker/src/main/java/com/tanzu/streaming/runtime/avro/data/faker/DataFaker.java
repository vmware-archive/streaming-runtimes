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

package com.tanzu.streaming.runtime.avro.data.faker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.avro.AvroFactory;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

public class DataFaker {

	protected static final Logger logger = LoggerFactory.getLogger(DataFaker.class);

	public static final boolean UTF_8_FOR_STRING = true;

	private DataFaker() {
	}

	public static List<GenericData.Record> generateRecords(Schema schema, int numberOfRecords) {
		return generateRecords(dataFaker(schema, numberOfRecords));
	}

	public static List<GenericData.Record> generateRecords(Schema schema, int numberOfRecords,
			SharedFieldValuesContext correlationContext,
			SharedFieldValuesContext.Mode correlationMode,
			String keyFieldName,
			long seed) {

		return generateRecords(dataFaker(schema,
				numberOfRecords,
				correlationContext,
				correlationMode,
				keyFieldName,
				seed));
	}

	public static List<GenericData.Record> generateRecords(AvroRandomDataFaker avroRandomDataFaker) {
		return StreamSupport.stream(
						Spliterators.spliteratorUnknownSize(avroRandomDataFaker.iterator(), Spliterator.ORDERED),
						false)
				.map(o -> (GenericData.Record) o)
				.collect(Collectors.toList());
	}

	public static AvroRandomDataFaker dataFaker(Schema schema, int numberOfRecords) {
		return new AvroRandomDataFaker(schema, numberOfRecords,
				!UTF_8_FOR_STRING, null, null, null, System.currentTimeMillis());
	}

	public static AvroRandomDataFaker dataFaker(Schema schema, int numberOfRecords,
			SharedFieldValuesContext correlationContext,
			SharedFieldValuesContext.Mode correlationMode,
			String keyFieldName,
			long seed) {
		return new AvroRandomDataFaker(schema, numberOfRecords, !UTF_8_FOR_STRING,
				correlationContext, correlationMode, keyFieldName, seed);
	}

	public static Schema resourceUriToAvroSchema(String schemaUri) {
		return resourceUriToAvroSchema(new DefaultResourceLoader().getResource(schemaUri));
	}

	public static Schema resourceUriToAvroSchema(Resource schemaResourceUri) {
		try {
			String schemaStr = new String(IOUtils.toByteArray(schemaResourceUri.getInputStream()));
			return textToAvroSchema(schemaStr);
		}
		catch (IOException e) {
			logger.error("Failed to parse resources: " + schemaResourceUri + " to Avro schema!", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates an Avro Schema instance from input schema string content.
	 * @param schemaContent Raw Schema text content.
	 * @return Returns Avro Schema.
	 */
	public static Schema textToAvroSchema(String schemaContent) {
		return new Schema.Parser().parse(convertYamlOrJsonToJson(schemaContent));
	}

	/**
	 * Converts the URI content into a string.
	 * @param resourceUri Resource URI. Support prefixes such as 'classpath:/', 'file:', 'http:', 'https:'.
	 * @return Returns the URI resource content as a string.
	 */
	public static String resourceUriToString(String resourceUri) {
		try {
			InputStream is = new DefaultResourceLoader().getResource(resourceUri).getInputStream();
			return new String(IOUtils.toByteArray(is));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts YAML or JSON back to JSON to let the Schema parser parse it.
	 * @param yamlOrJson Support either YAML or JSON as input.
	 * @return Returns JSON representation of the YAML or JSON input.
	 */
	public static String convertYamlOrJsonToJson(String yamlOrJson) {
		try {
			ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
			Object obj = yamlReader.readValue(yamlOrJson, Object.class);

			ObjectMapper jsonWriter = new ObjectMapper();
			return jsonWriter.writeValueAsString(obj);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts a list of Avro records into a list JSON strings.
	 * @param genericRecords Input records to convert.
	 * @return Returns a list of JSON strings that represent the input records.
	 */
	public static List<Object> toJson(List<GenericRecord> genericRecords) {
		return genericRecords.stream()
				.map(DataFaker::toJson)
				.collect(Collectors.toList());
	}

	/**
	 * Converts a single Avro GenericRecord into JSON object.
	 * @param genericRecord record to covert
	 * @return Returns JSON string representation of the input record.
	 */
	public static Object toJson(GenericRecord genericRecord) {
		try {
			ObjectMapper mapper = new ObjectMapper(new AvroFactory());

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(genericRecord.getSchema());
				BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
				writer.write(genericRecord, encoder);
				encoder.flush();

				return mapper.readerFor(ObjectNode.class)
						.with(new AvroSchema(genericRecord.getSchema()))
						.readValue(outputStream.toByteArray());
			}
		}
		catch (Exception e) {
			logger.error("Failed to convert GenericRecord into JSON", e);
			throw new RuntimeException(e);
		}
	}

}
