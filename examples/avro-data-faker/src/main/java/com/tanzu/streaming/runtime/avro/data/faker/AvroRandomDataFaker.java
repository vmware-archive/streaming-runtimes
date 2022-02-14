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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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

import org.springframework.util.StringUtils;


/** Generates schema data as Java objects with random values.
 * It forks from the org.apache.avro.RandomData to add com.github.javafaker.Faker support (via doc annotations)
 * and can correlate filed values from different schemas.
 */
public class AvroRandomDataFaker implements Iterable<GenericData.Record> {

	protected static final Logger logger = LoggerFactory.getLogger(AvroRandomDataFaker.class);

	public static final String USE_DEFAULT = "use-default";

	private final Schema root;
	private final int count;
	private final boolean utf8ForString;
	private final Faker faker;
	private final Random random;

	private final SharedFieldValuesContext sharedFieldValuesContext;
	private final SharedFieldValuesContext.Mode sharedFieldValuesMode;

	/**
	 * If the keyFieldName is set the generator will always return the same Record per same key value.
	 * For this the generator keeps a map of all generated records per unique key in the keyedRecords. If new
	 * record is produced that has a record for the same key in the map the existing records is returned.
	 *
	 * NOTE: if the key range is big this could lead to OOM.
	 */
	private final String keyFieldName;
	private final ConcurrentHashMap<Object, GenericData.Record> keyedRecords;

	public AvroRandomDataFaker(Schema schema, int count, boolean utf8ForString) {
		this(schema, count, utf8ForString, null, null, null, System.currentTimeMillis());
	}

	public AvroRandomDataFaker(Schema schema, int count, boolean utf8ForString,
			SharedFieldValuesContext sharedFieldValuesContext,
			SharedFieldValuesContext.Mode sharedFiledValuesMode,
			String keyFieldName,
			long seed) {
		this.root = schema;
		this.random = new Random(seed);
		this.count = count;
		this.utf8ForString = utf8ForString;
		this.faker = new Faker(this.random);
		this.sharedFieldValuesContext = sharedFieldValuesContext;
		this.sharedFieldValuesMode = sharedFiledValuesMode;

		this.keyFieldName = keyFieldName;
		this.keyedRecords = new ConcurrentHashMap();
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

				GenericData.Record record = (GenericData.Record) generate(null, root, random, 0);

				if (record != null && keyFieldName != null) {
					Object keyValue = record.get(keyFieldName);
					if (keyValue != null) {
						keyedRecords.putIfAbsent(keyValue, record);
						record = keyedRecords.get(keyValue);
					}
					else {
						logger.warn(String.format("No keyFieldName[%s] value found in record",  keyFieldName, record));
					}
				}
				return record;
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
		return StringUtils.hasText(doc) ? faker.expression(doc) : null;
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
