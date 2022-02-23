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

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tanzu.streaming.runtime.data.generator.context.SharedFieldValuesContext;
import com.tanzu.streaming.runtime.data.generator.context.SpELTemplateParserContext;
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

/**
 * Generates Avro Schema data as GenericData.Record objects with random but realistic fake values.
 * You can use JSON or YAML to define the input schema. The DataUtil provides different options to convert the
 * generated records into JSON, AVRO or YAML.
 *
 * Use the fields "doc" to hint what data to generate. Both DataFaker or SpEL expressions can be used like this:
 * <code>
 *   - name: id
 *     type: string
 *     doc: "#{options.option '100-00-0000','200-00-0000'}"
 * </code>
 *
 * <code>
 *   - name: email
 *     type: string
 *     doc: "#{internet.emailAddress '[[#name.toLowerCase().replaceAll(\"\\s+\", \".\")]]'}"
 * </code>
 *
 * Special support is provided to ensure inter record field dependency generator as well as sharing context between
 * multiple data generators.
 *
 * Without hints annotations the output would be similar to org.apache.avro.RandomData!
 *
 * @author Christain Tzolov (christian@tzolov.net)
 */
public class DataGenerator implements Iterable<GenericData.Record> {

	protected static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);

	private static final Charset UTF8 = StandardCharsets.UTF_8;

	public static final boolean UTF_8_FOR_STRING = true;
	/**
	 * Reserved variable names to be used in the Record's doc and applied across all fields.
	 */
	public static final String DEFAULT_VARIABLE_NAME = "default";
	public static final String FAKER_VARIABLE_NAME = "faker";
	public static final String UNIQUE_ON_VARIABLE_NAME = "unique_on";
	public static final String TO_SHARE_VARIABLE_NAME = "to_share";
	public static final String SHARED_VARIABLE_NAME = "shared";

	public static final String TO_SHARE_FIELDS_SEPARATOR = ",";

	/**
	 * For Record and Map Docs you can line expressions like:
	 * "doc": "k1=v1;k2=v2;...;kN=vN"
	 *
	 * The LINE_PAIRS_SEPARATOR defines the separator between the key-value pairs in the line.
	 * The LINE_PAIR_KEYVALUES_SEPARATOR splints each key-value pair into its Key and Value parts.
	 */
	public static final String LINE_PAIRS_SEPARATOR = ";";
	public static final String LINE_PAIR_KEYVALUES_SEPARATOR = "=";

	/**
	 * For the Map Avro type you can define length, the key and the value expressions. For example:
	 * <code>
	 *     {
	 *       "name": "myMap",
	 *       "type": {"type": "map", "values": "string"},
	 *       "doc": "key=#{id_number.valid};value=#{commerce.department};length=#{number.number_between '0','10'}",
	 *     }
	 * </code>
	 *
	 * Here the <code>length</code> defines the number of map entries to be generated, and the
	 * <code>key</code> and <code>value</code> attribute expressions are used to generate the key and value for each map entry.
	 *
	 * Note that the LINE_PAIRS_SEPARATOR and the LINE_PAIR_KEYVALUES_SEPARATOR are used to define the key/value pairs.
	 *
	 * All other attributes are ignored.
	 */
	public static final String MAP_KEY = "key";
	public static final String MAP_VALUE = "value";
	public static final String MAP_LENGTH = "length";

	private final Schema schema;
	private final int numberOfRecords;
	private final boolean utf8ForString;
	private final Faker faker;
	private final Random random;

	/**
	 * If the "to_share=myField1:myField2" is set on Record's doc level the generator will preserve the generated
	 * values for those fields in external storage (shared field context).
	 * Those values can be accessed via [[#shared.field('my-field-name')]] expression. Later returns a RANDOM value
	 * from withing the set of values for this field name stored in the shared context.
	 * The read/write access to the shared-field-context is threadsafe, permitting adding and reading those values in
	 * parallel.
	 */
	private final SharedFieldValuesContext sharedFieldValuesContext;

	/**
	 * When the "unique_on=my-field-name" is set on record-doc level, the generator will ensure that all records with
	 * the same "my-field-name" have exactly the same other fields as well.
	 */
	private final ConcurrentHashMap<Object, GenericRecord> uniqueOnFieldNameRecords;

	/**
	 * Spring Expression Language (SpEL) configuration.
	 * You can use [[SpEL]] in our docs alone or along Faker expressions.
	 * By default, the SpEL are resolved before the Faker expression, though you can run Faker expressions from
	 * withing the SpEL like: [[#faker.idNumber().valid()]].
	 */
	private final StandardEvaluationContext spelContext;
	private final SpELTemplateParserContext spelTemplateContext;
	private final SpelExpressionParser spelParser;

	public DataGenerator(Schema schema, int numberOfRecords) {
		this(schema, numberOfRecords, !UTF_8_FOR_STRING, null, System.currentTimeMillis());
	}

	public DataGenerator(Schema schema, int numberOfRecords, boolean utf8ForString) {
		this(schema, numberOfRecords, utf8ForString, null, System.currentTimeMillis());
	}

	public DataGenerator(Schema schema, int numberOfRecords,
			SharedFieldValuesContext sharedFieldValuesContext) {
		this(schema, numberOfRecords, !UTF_8_FOR_STRING, sharedFieldValuesContext, System.currentTimeMillis());
	}

	public DataGenerator(Schema schema, int numberOfRecords,
			SharedFieldValuesContext sharedFieldValuesContext, long seed) {
		this(schema, numberOfRecords, !UTF_8_FOR_STRING, sharedFieldValuesContext, seed);
	}

	public DataGenerator(Schema schema, int numberOfRecords, boolean utf8ForString,
			SharedFieldValuesContext sharedFieldValuesContext, long seed) {

		this.schema = schema;
		this.random = new Random(seed);
		this.numberOfRecords = numberOfRecords;
		this.utf8ForString = utf8ForString;
		this.faker = new Faker(this.random);
		this.sharedFieldValuesContext = sharedFieldValuesContext;

		this.uniqueOnFieldNameRecords = new ConcurrentHashMap();

		this.spelContext = new StandardEvaluationContext();

		// All instances will have access to the Faker via [[#faker.xxxx]]
		this.spelContext.setVariable(FAKER_VARIABLE_NAME, faker);

		// All instances will have access to the sharedFieldContext via [[#shared.field('my-field-name')]]
		this.spelContext.setVariable(SHARED_VARIABLE_NAME, sharedFieldValuesContext);

		this.spelTemplateContext = new SpELTemplateParserContext();
		this.spelParser = new SpelExpressionParser();
	}

	public Schema getSchema() {
		return schema;
	}

	@Override
	public Iterator<GenericData.Record> iterator() {
		return new Iterator<>() {
			private int index;

			@Override
			public boolean hasNext() {
				return index < numberOfRecords;
			}

			@Override
			public GenericData.Record next() {
				index++;
				return (GenericData.Record) generate(null, schema, random, 0);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@SuppressWarnings(value = "unchecked")
	private Object generate(String doc, Schema schema, Random random, int d) {
		switch (schema.getType()) {
		case RECORD:

			GenericRecord record = new GenericData.Record(schema);

			Map<String, String> resolvedRecordKeyValues = null;

			try {
				//	Extract the key/value expressions from Record's doc.
				//	Multiple key=value pairs are allowed, using the ';' separator.
				//	The kay/values resolved from the record's Doc are kept in the SpEL context and can be used by the  field expressions.
				//	The `unique_on` name is reserved to set the keyed field name for the record.
				//	The `to_share` name is reserved for sharing field values with multiple generators.
				//	The `faker` name is reserved for holding a Faker instance. Use [[#faker.xxx()]] expressions to use it.
				resolvedRecordKeyValues = parseDocToKeyValuePairs(record.getSchema().getDoc())
						.entrySet().stream().collect(
								Collectors.toMap(
										entry -> resolveDocExpressions(entry.getKey()),
										entry -> resolveDocExpressions(entry.getValue())
								)
						);

				// Add the parsed record key/values paris to the SpEL Context.
				// Paris are added only for the duration of this record processing, then they are removed!
				// E.g. those values are not shared across multiple Record instance generations.
				resolvedRecordKeyValues.forEach(this.spelContext::setVariable);

				for (Schema.Field field : schema.getFields()) {

					// Allow accessing the field default value (when present) via the [[#default]] expression.
					if (field.hasDefaultValue()) {
						this.spelContext.setVariable(DEFAULT_VARIABLE_NAME, GenericData.get().getDefaultValue(field));
					}

					Object value = generate(field.doc(), field.schema(), random, d + 1);

					record.put(field.name(), value);

					if (this.sharedFieldValuesContext != null && fieldNamesToRetainValuesFor(resolvedRecordKeyValues).contains(field.name())) {
						String sharedKey = String.format("%s.%s", record.getSchema().getName(), field.name())
								.toLowerCase();
						this.sharedFieldValuesContext.addValue(sharedKey, value);
					}

					// Remove field's default value from the spel context.
					this.spelContext.setVariable(DEFAULT_VARIABLE_NAME, null);
				}

				// If the record's "unique_on" field-name is set and a record for that field has already been generated,
				// then return the existing record. This prevents having multiple different records sharing the same unique_on field!
				record = this.replaceWithUniqueFieldRecord(record, resolvedRecordKeyValues.get(UNIQUE_ON_VARIABLE_NAME));
			}
			catch (Exception e) {
				logger.error(String.format("Returns a NULL record for Schema: %s", schema.getName()), e);
				return null;
			}
			finally {
				// Remove the record's k/v variables from the SpEL context.
				if (resolvedRecordKeyValues != null) {
					resolvedRecordKeyValues.forEach((key, value) -> this.spelContext.setVariable(key, null));
				}
			}

			return record;
		case ENUM:
			List<String> symbols = schema.getEnumSymbols();
			return new GenericData.EnumSymbol(schema, symbols.get(random.nextInt(symbols.size())));
		case ARRAY:
			int length = (random.nextInt(5) + 2) - d;
			@SuppressWarnings("rawtypes")
			GenericArray<Object> array = new GenericData.Array(Math.max(length, 0), schema);
			for (int i = 0; i < length; i++) {
				array.add(generate(doc, schema.getElementType(), random, d + 1));
			}
			return array;
		case MAP:
			Map<String, String> unresolvedKeyValues = parseDocToKeyValuePairs(doc);
			int mapLength = Integer.parseInt(resolveDocExpressions(
					unresolvedKeyValues.getOrDefault(MAP_LENGTH, "" + Math.max((random.nextInt(5) + 2) - d, 0))));
			Map<Object, Object> map = new HashMap(mapLength);
			for (int i = 0; i < mapLength; i++) {
				Object key = unresolvedKeyValues.containsKey(MAP_KEY) ?
						resolveDocExpressions(unresolvedKeyValues.get(MAP_KEY)) : randomString(random, 40);
				String unresolvedValue = unresolvedKeyValues.get(MAP_VALUE); // we do NOT want to resolve this here!
				map.put(key, generate(unresolvedValue, schema.getValueType(), random, d + 1));
			}
			return map;
		case UNION:
			List<Schema> types = schema.getTypes();
			return generate(doc, types.get(random.nextInt(types.size())), random, d);
		case FIXED:
			byte[] bytes = StringUtils.hasText(doc) ?
					resolveDocExpressions(doc).getBytes() : new byte[schema.getFixedSize()];
			random.nextBytes(bytes);
			return new GenericData.Fixed(schema, bytes);
		case STRING:
			return StringUtils.hasText(doc) ?
					resolveDocExpressions(doc) : randomString(random, 40);
		case BYTES:
			return StringUtils.hasText(doc) ?
					resolveDocExpressions(doc).getBytes() : randomBytes(random, 40);
		case INT:
			return StringUtils.hasText(doc) ?
					Integer.parseInt(resolveDocExpressions(doc)) : random.nextInt();
		case LONG:
			return StringUtils.hasText(doc) ?
					Long.parseLong(resolveDocExpressions(doc)) : random.nextLong();
		case FLOAT:
			return StringUtils.hasText(doc) ?
					Float.parseFloat(resolveDocExpressions(doc)) : random.nextFloat();
		case DOUBLE:
			return StringUtils.hasText(doc) ?
					Double.parseDouble(resolveDocExpressions(doc)) : random.nextDouble();
		case BOOLEAN:
			return StringUtils.hasText(doc) ?
					Boolean.parseBoolean(resolveDocExpressions(doc)) : random.nextBoolean();
		case NULL:
			return null;
		default:
			throw new RuntimeException("Unknown type: " + schema);
		}
	}

	/**
	 * Parses the `to_share` Record level property. Later lists the field names for which the values are retained
	 * and can be shared with other records from the same or other type. For example `to_share=fieldNam1:fieldName2:fieldName3...`
	 *
	 * @param recordKeyValues The parsed Record's doc expression that contains comma separated key/value pairs like this:
	 *                         the form: `key1=value1,key2=value2...`
	 * @return Returns the set of field names listed under the to_share property or empty set otherwise.
	 */
	private Set<String> fieldNamesToRetainValuesFor(Map<String, String> recordKeyValues) {
		Set<String> retainFieldNames = new HashSet<>();
		if (recordKeyValues.containsKey(TO_SHARE_VARIABLE_NAME)) {
			String[] fieldNames = recordKeyValues.get(TO_SHARE_VARIABLE_NAME).split(TO_SHARE_FIELDS_SEPARATOR);
			if (fieldNames.length > 0) {
				retainFieldNames.addAll(Set.of(fieldNames));
			}
		}
		return retainFieldNames;
	}

	/**
	 * If the key field name is set the generator will always return the same Record per same key value.
	 * For this the generator keeps a map of all generated records per unique key in the keyedRecords. If new
	 * record is produced that has a record for the same key in the map the existing records is returned.
	 *
	 * NOTE: if the key range is big this could lead to OOM.
	 */
	private GenericRecord replaceWithUniqueFieldRecord(GenericRecord record, String keyFieldName) {
		if (record != null && StringUtils.hasText(keyFieldName)) {
			Object keyValue = record.get(keyFieldName);
			if (keyValue != null) {
				this.uniqueOnFieldNameRecords.putIfAbsent(keyValue, record);
				record = this.uniqueOnFieldNameRecords.get(keyValue);
			}
			else {
				logger.warn(String.format("No keyFieldName[%s] value found in record %s", keyFieldName, record));
			}
		}
		return record;
	}

	private String resolveDocExpressions(String doc) {
		if (doc == null) {
			throw new IllegalArgumentException("Null doc");
		}
		if (!StringUtils.hasText(doc)) {
			return doc;
		}
		String resolvedSpELDoc = this.spelParser.parseExpression(doc, this.spelTemplateContext)
				.getValue(this.spelContext, String.class);

		if (resolvedSpELDoc == null) {
			throw new IllegalStateException(String.format("Null SpEL resolution for doc:%s ", doc));
		}
		return faker.expression(resolvedSpELDoc);
	}

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

	/**
	 * Optimization, since the Schema docs are immutable.
	 */
	private ConcurrentHashMap<String, Map<String, String>> unresolvedDocKeyValuePairsCache = new ConcurrentHashMap<>();

	/**
	 * Parse multi key=value pairs Doc expressions. Later are used either in the Record or Map docs.
	 * Expected format is: "doc": "key1=value1;key2=value2...keyN=valueN".
	 * The keys and the values can be faker/spel expressions. This parser does NOT resolve such expressions.
	 * @param doc Record or Map multi-pair expression.
	 * @return Returns a map of the key and values found in the doc. Both the key and the values are raw (unresolved).
	 */
	private Map<String, String> parseDocToKeyValuePairs(String doc) {

		if (!StringUtils.hasText(doc)) {
			return Collections.emptyMap();
		}

		if (this.unresolvedDocKeyValuePairsCache.containsKey(doc)) {
			return this.unresolvedDocKeyValuePairsCache.get(doc);
		}

		Map<String, String> keyValues = new HashMap<>();

		Stream.of(doc.split(LINE_PAIRS_SEPARATOR)).forEach(keyValuePair -> {
			String[] keyValue = keyValuePair.split(LINE_PAIR_KEYVALUES_SEPARATOR);
			if (keyValue.length == 2) {
				String unresolvedKey = keyValue[0];
				Assert.hasText(unresolvedKey, "Empty key is not allowed!");
				unresolvedKey = unresolvedKey.strip();

				String unresolvedValue = keyValue[1];
				Assert.notNull(unresolvedValue, "Null key value!");
				unresolvedValue = unresolvedValue.strip();

				keyValues.put(unresolvedKey, unresolvedValue);
			}
			else {
				throw new IllegalArgumentException("Illegal key-value pair expression for: " + keyValuePair);
			}
		});

		this.unresolvedDocKeyValuePairsCache.putIfAbsent(doc, keyValues);

		return keyValues;
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
			for (Object datum : new DataGenerator(sch, Integer.parseInt(args[2]), false)) {
				writer.append(datum);
			}
		}
		finally {
			writer.close();
		}
	}
}
