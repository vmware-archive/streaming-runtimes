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

package com.tanzu.streaming.runtime.data.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.avro.AvroFactory;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
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

public class DataUtil {

	protected static final Logger logger = LoggerFactory.getLogger(DataUtil.class);

	private static final ObjectMapper yamlMapper =
			new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

	private static final ObjectMapper avroMapper = new ObjectMapper(new AvroFactory());

	private DataUtil() {
	}


	public static Schema uriToSchema(String schemaUri) {
		return resourceToSchema(new DefaultResourceLoader().getResource(schemaUri));
	}

	public static Schema resourceToSchema(Resource schemaResourceUri) {
		try {
			String schemaStr = new String(IOUtils.toByteArray(schemaResourceUri.getInputStream()));
			return contentToSchema(schemaStr);
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
	public static Schema contentToSchema(String schemaContent) {
		return new Schema.Parser().parse(yamlOrJsonToJson(schemaContent));
	}

	/**
	 * Converts YAML or JSON back to JSON to let the Schema parser parse it.
	 * @param yamlOrJson Support either YAML or JSON as input.
	 * @return Returns JSON representation of the YAML or JSON input.
	 */
	public static String yamlOrJsonToJson(String yamlOrJson) {
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
//	public static String toJsonArray(List<GenericData.Record> genericRecords) {
//		try {
//			return new ObjectMapper().writeValueAsString(toJsonObjects(genericRecords));
//		}
//		catch (JsonProcessingException e) {
//			logger.error("Failed to convert GenericRecords into JSON", e);
//			throw new RuntimeException(e);
//		}
//	}

	/**
	 * Converts a single Avro GenericRecord into JSON object.
	 * @param genericRecord record to covert
	 * @return Returns JSON string representation of the input record.
	 */
	public static String toJson(GenericRecord genericRecord) {
		return toJsonObjectNode(genericRecord).toString();
	}

//	public static String toYamlArray(List<GenericData.Record> genericRecords) {
//		try {
//			List<ObjectNode> jsonObjects = toJsonObjects(genericRecords);
//			return yamlMapper.writeValueAsString(jsonObjects);
//		}
//		catch (JsonProcessingException e) {
//			logger.error("Failed to convert GenericRecords into YAML", e);
//			throw new RuntimeException(e);
//		}
//	}

	/**
	 *
	 * @param genericRecord
	 * @return
	 */
	public static String toYaml(GenericRecord genericRecord) {
		try {
			return yamlMapper.writeValueAsString(toJsonObjectNode(genericRecord));
		}
		catch (JsonProcessingException e) {
			logger.error("Failed to convert GenericRecord into YAML", e);
			throw new RuntimeException(e);
		}
	}

//	private static List<ObjectNode> toJsonObjects(List<GenericData.Record> genericRecords) {
//		return genericRecords.stream()
//				.map(DataUtil::toJsonObjectNode)
//				.collect(Collectors.toList());
//	}

	/**
	 * Converts a single Avro GenericRecord into JSON object.
	 * @param genericRecord record to covert
	 * @return Returns JSON string representation of the input record.
	 */
	public static ObjectNode toJsonObjectNode(GenericRecord genericRecord) {
		try {
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(genericRecord.getSchema());
				BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
				writer.write(genericRecord, encoder);
				encoder.flush();

				return avroMapper.readerFor(ObjectNode.class)
						.with(new AvroSchema(genericRecord.getSchema()))
						.readValue(outputStream.toByteArray());
			}
		}
		catch (Exception e) {
			logger.error("Failed to convert GenericRecord into JSON", e);
			throw new RuntimeException(e);
		}
	}

	public static void print(DataGenerator dataGenerator) {
		toList(dataGenerator).forEach(System.out::println);
	}

	public static List<GenericData.Record> toList(DataGenerator avroRandomData) {
		return StreamSupport.stream(
						Spliterators.spliteratorUnknownSize(avroRandomData.iterator(), Spliterator.ORDERED),
						false)
				.collect(Collectors.toList());
	}
}
