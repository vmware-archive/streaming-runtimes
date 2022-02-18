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

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.tanzu.streaming.runtime.avro.data.faker.util.SharedFieldValuesContext;
import com.tanzu.streaming.runtime.avro.data.faker.util.SpELTemplateParserContext;
import net.datafaker.Faker;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/** Generates schema data as Java objects with random values.
 * It forks from the org.apache.avro.RandomData to add com.github.javafaker.Faker support (via doc annotations)
 * and can correlate filed values from different schemas.
 */
public class AvroRandomDataFaker implements Iterable<GenericData.Record> {

	protected static final Logger logger = LoggerFactory.getLogger(AvroRandomDataFaker.class);

	public static final String USE_DEFAULT = "use-default";

	public static final String KEY_FIELD_NAME = "key";

	private final Schema root;
	private final int count;
	private final boolean utf8ForString;
	private final Faker faker;
	private final Random random;

	private final SharedFieldValuesContext sharedFieldValuesContext;
	private final SharedFieldValuesContext.Mode sharedFieldValuesMode;


	/**
	 *
	 */
	private final ConcurrentHashMap<Object, GenericRecord> keyedRecords;
	private final StandardEvaluationContext spelContext;
	private final SpELTemplateParserContext spelTemplateContext;
	private final SpelExpressionParser spelParser;

	public AvroRandomDataFaker(Schema schema, int count, boolean utf8ForString) {
		this(schema, count, utf8ForString, null, null, System.currentTimeMillis());
	}

	public AvroRandomDataFaker(Schema schema, int count, boolean utf8ForString,
			SharedFieldValuesContext sharedFieldValuesContext,
			SharedFieldValuesContext.Mode sharedFiledValuesMode,
			long seed) {
		this.root = schema;
		this.random = new Random(seed);
		this.count = count;
		this.utf8ForString = utf8ForString;
		this.faker = new Faker(this.random);
		this.sharedFieldValuesContext = sharedFieldValuesContext;
		this.sharedFieldValuesMode = sharedFiledValuesMode;

		this.keyedRecords = new ConcurrentHashMap();

		this.spelContext = new StandardEvaluationContext();
		this.spelContext.setVariable("faker", faker);
		this.spelTemplateContext = new SpELTemplateParserContext();
		this.spelParser = new SpelExpressionParser();
	}

	@Override
	public Iterator<GenericData.Record> iterator() {
		return new Iterator<>() {
			private int n;

			@Override
			public boolean hasNext() {
				return n < count;
			}

			@Override
			public GenericData.Record next() {
				n++;
				return (GenericData.Record) generate(null, root, random, 0);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@SuppressWarnings(value = "unchecked")
	private Object generate(String fakerValue, Schema schema, Random random, int d) {
		switch (schema.getType()) {
		case RECORD:
			GenericRecord record = new GenericData.Record(schema);
			Map<String, String> recordKeyValueExpressions =
					this.addRecordExpressionsToSpELContext(record.getSchema().getDoc());
			for (Schema.Field field : schema.getFields()) {

				Object value;
				if (isUseSharedFieldValues(field.name())) {
					value = this.sharedFieldValuesContext.getRandomValue(field.name(), random);
				}
				else if (field.getObjectProp(USE_DEFAULT) != null) {
					value = GenericData.get().getDefaultValue(field);
				}
				else {
					value = generate(fakerExpression(field.doc()), field.schema(), random, d + 1);
				}

				record.put(field.name(), value);

				if (isSaveSharedFieldValues(field.name())) {
					this.sharedFieldValuesContext.addValue(field.name(), value);
				}
			}

			record = this.replaceWithKeyedFieldRecord(record, recordKeyValueExpressions.get(KEY_FIELD_NAME));

			this.removeRecordExpressionsFromSpELContext(recordKeyValueExpressions);

			return record;
		case ENUM:
			List<String> symbols = schema.getEnumSymbols();
			return new GenericData.EnumSymbol(schema, symbols.get(random.nextInt(symbols.size())));
		case ARRAY:
			int length = (random.nextInt(5) + 2) - d;
			@SuppressWarnings("rawtypes")
			GenericArray<Object> array = new GenericData.Array(length <= 0 ? 0 : length, schema);
			for (int i = 0; i < length; i++)
				array.add(generate(fakerValue, schema.getElementType(), random, d + 1));
			return array;
		case MAP:
			length = (random.nextInt(5) + 2) - d;
			Map<Object, Object> map = new HashMap<>(length <= 0 ? 0 : length);
			for (int i = 0; i < length; i++) {
				map.put(randomString(random, 40), generate(fakerValue, schema.getValueType(), random, d + 1));
			}
			return map;
		case UNION:
			List<Schema> types = schema.getTypes();
			return generate(fakerValue, types.get(random.nextInt(types.size())), random, d);
		case FIXED:
			byte[] bytes = StringUtils.hasText(fakerValue) ?
					fakerValue.getBytes() : new byte[schema.getFixedSize()];
			random.nextBytes(bytes);
			return new GenericData.Fixed(schema, bytes);
		case STRING:
			return StringUtils.hasText(fakerValue) ?
					fakerValue : randomString(random, 40);
		case BYTES:
			return StringUtils.hasText(fakerValue) ?
					fakerValue.getBytes() : randomBytes(random, 40);
		case INT:
			return StringUtils.hasText(fakerValue) ?
					Integer.parseInt(fakerValue) : random.nextInt();
		case LONG:
			return StringUtils.hasText(fakerValue) ?
					Long.parseLong(fakerValue) : random.nextLong();
		case FLOAT:
			return StringUtils.hasText(fakerValue) ?
					Float.parseFloat(fakerValue) : random.nextFloat();
		case DOUBLE:
			return StringUtils.hasText(fakerValue) ?
					Double.parseDouble(fakerValue) : random.nextDouble();
		case BOOLEAN:
			return StringUtils.hasText(fakerValue) ?
					Boolean.parseBoolean(fakerValue) : random.nextBoolean();
		case NULL:
			return null;
		default:
			throw new RuntimeException("Unknown type: " + schema);
		}
	}

	/**
	 * If the key field name is set the generator will always return the same Record per same key value.
	 * For this the generator keeps a map of all generated records per unique key in the keyedRecords. If new
	 * record is produced that has a record for the same key in the map the existing records is returned.
	 *
	 * NOTE: if the key range is big this could lead to OOM.
	 */
	private GenericRecord replaceWithKeyedFieldRecord(GenericRecord record, String keyFieldName) {
		if (record != null && StringUtils.hasText(keyFieldName)) {
			Object keyValue = record.get(keyFieldName);
			if (keyValue != null) {
				this.keyedRecords.putIfAbsent(keyValue, record);
				record = this.keyedRecords.get(keyValue);
			}
			else {
				logger.warn(String.format("No keyFieldName[%s] value found in record", keyFieldName, record));
			}
		}
		return record;
	}

	private boolean isUseSharedFieldValues(String fieldName) {
		return this.sharedFieldValuesMode == SharedFieldValuesContext.Mode.CONSUMER
				&& this.sharedFieldValuesContext != null
				&& this.sharedFieldValuesContext.getSharedFieldNames().contains(fieldName)
				&& this.sharedFieldValuesContext.getState().containsKey(fieldName);
	}

	private boolean isSaveSharedFieldValues(String fieldName) {
		return this.sharedFieldValuesMode == SharedFieldValuesContext.Mode.PRODUCER
				&& this.sharedFieldValuesContext != null
				&& this.sharedFieldValuesContext.getSharedFieldNames().contains(fieldName);
	}

	private String fakerExpression(String doc) {
		if (StringUtils.hasText(doc)) {
			String resolvedSpELDoc = this.spelParser.parseExpression(doc, this.spelTemplateContext)
					.getValue(this.spelContext, String.class);
			return faker.expression(resolvedSpELDoc);
		}
		return null;
	}

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private Object randomString(Random random, int maxLength) {
		int length = random.nextInt(maxLength);
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte) ('a' + random.nextInt('z' - 'a'));
		}
		return utf8ForString ? new Utf8(bytes) : new String(bytes, UTF8);
	}

	private static ByteBuffer randomBytes(Random rand, int maxLength) {
		ByteBuffer bytes = ByteBuffer.allocate(rand.nextInt(maxLength));
		((Buffer) bytes).limit(bytes.capacity());
		rand.nextBytes(bytes.array());
		return bytes;
	}

	private void removeRecordExpressionsFromSpELContext(Map<String, String> recordKeyValues) {
		recordKeyValues.entrySet().stream().forEach(e -> this.spelContext.setVariable(e.getKey(), null));
	}

	private Map<String, String> addRecordExpressionsToSpELContext(String recordDoc) {

		if (!StringUtils.hasText(recordDoc)) {
			return Collections.emptyMap();
		}

		Map<String, String> recordKeyValues = new HashMap<>();

		String[] keyValuePairs = recordDoc.split(",");
		if (keyValuePairs.length > 0) {
			for (String keyValuePair : keyValuePairs) {
				String[] keyValue = keyValuePair.split("=");
				if (keyValue.length == 2) {
					String unresolvedKey = keyValue[0];
					Assert.hasText(unresolvedKey, "Empty key is not allowed!");
					String unresolvedValue = keyValue[1];
					Assert.notNull(unresolvedValue, "Null key value!");

					String spELResolvedKey = this.spelParser.parseExpression(unresolvedKey, spelTemplateContext)
							.getValue(this.spelContext, String.class);
					String resolvedKey = this.faker.expression(spELResolvedKey);

					String spELResolvedValue = this.spelParser.parseExpression(unresolvedValue, spelTemplateContext)
							.getValue(this.spelContext, String.class);
					String resolvedValue = this.faker.expression(spELResolvedValue);

					logger.info(String.format("Record Doc %s=%s", resolvedKey, resolvedValue));

					this.spelContext.setVariable(resolvedKey, resolvedValue);

					recordKeyValues.put(resolvedKey, resolvedValue);
				}
				else {
					throw new IllegalArgumentException("Illegal key-value pair expression for: " + keyValuePair);
				}
			}
		}

		return recordKeyValues;
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3 || args.length > 4) {
			System.out.println("Usage: RandomData <schemafile> <outputfile> <count> [codec]");
			System.exit(-1);
		}
		Schema sch = new Schema.Parser().parse(new File(args[0]));
		DataFileWriter<Object> writer = new DataFileWriter<>(new GenericDatumWriter<>());
		writer.setCodec(CodecFactory.fromString(args.length >= 4 ? args[3] : "null"));
		writer.create(sch, new File(args[1]));
		try {
			for (Object datum : new AvroRandomDataFaker(sch, Integer.parseInt(args[2]), false)) {
				writer.append(datum);
			}
		}
		finally {
			writer.close();
		}
	}
}
