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

import java.util.List;
import java.util.stream.Collectors;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Converts a standard SQL CREATE TABLE statement into single-record Avro schema.
 */
@Component
public class InlineSqlToAvroConverter implements DataSchemaAvroConverter {

	public static final String TYPE = "sql";

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

		return parse(context.getDataSchemaContext().getInline().getSchema());
	}

	/**
	 * Converts a standard CREATE TABLE SQL statement into single-record Avro Schema.
	 *
	 * @param createTableSql Input DDL statement to convert into Avro schema. (only CREATE TABLE statement is supported).
	 * @return Returns Avro schema that represent the input DDL.
	 */
	public Schema parse(String createTableSql) {

		try {
			Statements stmt = CCJSqlParserUtil.parseStatements(createTableSql);
			CreateTable createTable = (CreateTable) stmt.getStatements().get(0);

			SchemaBuilder.FieldAssembler<Schema> recordFieldBuilder = SchemaBuilder
					.record(createTable.getTable().getName())
					.namespace("com.tanzu.streaming.runtime.sql.generated." + createTable.getTable().getName()
							.toLowerCase())
					.fields();

			for (ColumnDefinition cd : createTable.getColumnDefinitions()) {
				String columnName = cd.getColumnName().replace("`", "");
				String columnType = cd.getColDataType().getDataType();
				List<String> typeArgs = cd.getColDataType().getArgumentsStringList();
				if (!CollectionUtils.isEmpty(typeArgs)) {
					columnType = columnType.trim() + typeArgs.stream().collect(Collectors.joining(",", "(", ")"));
				}
				boolean isNullable = isNullable(cd.getColumnSpecs());
				Schema avroFieldType = convertToAvroSchema(columnType, isNullable);
				recordFieldBuilder.name(columnName).type(avroFieldType).noDefault();
			}

			return recordFieldBuilder.endRecord();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isNullable(List<String> columnOptions) {
		if (CollectionUtils.isEmpty(columnOptions)) {
			return true;
		}
		return !columnOptions.containsAll(List.of("NOT", "NULL"));
	}

	/**
	 * Somewhat naive conversion of SQL to Avro types.
	 * Currently, the MAP and ARRAY types are not supported.
	 *
	 * @param sqlType Input SQL type to convert.
	 * @param nullable Indicate if the generated avro type is nullable or not.
	 * @return Returns an Avro type corresponding to the input SQL type.
	 */
	private Schema convertToAvroSchema(String sqlType, boolean nullable) {
		int precision;
		switch (sqlType) {
		case "NULL":
			return SchemaBuilder.builder().nullType();
		case "BOOLEAN":
			Schema bool = SchemaBuilder.builder().booleanType();
			return nullable ? nullableSchema(bool) : bool;
		case "TINYINT":
		case "SMALLINT":
		case "INTEGER":
			Schema integer = SchemaBuilder.builder().intType();
			return nullable ? nullableSchema(integer) : integer;
		case "BIGINT":
			Schema bigint = SchemaBuilder.builder().longType();
			return nullable ? nullableSchema(bigint) : bigint;
		case "FLOAT":
			Schema f = SchemaBuilder.builder().floatType();
			return nullable ? nullableSchema(f) : f;
		case "DOUBLE":
			Schema d = SchemaBuilder.builder().doubleType();
			return nullable ? nullableSchema(d) : d;
		case "STRING":
		case "CHAR":
		case "VARCHAR":
			Schema str = SchemaBuilder.builder().stringType();
			return nullable ? nullableSchema(str) : str;
		case "BINARY":
		case "VARBINARY":
			Schema binary = SchemaBuilder.builder().bytesType();
			return nullable ? nullableSchema(binary) : binary;
		case "DATE":
			// use int to represents Date
			Schema date = LogicalTypes.date().addToSchema(SchemaBuilder.builder().intType());
			return nullable ? nullableSchema(date) : date;
		case "TIMESTAMP":
		case "TIMESTAMP(3)":
			precision = 3;
			if (precision > 3) {
				throw new IllegalArgumentException(
						"Avro does not support TIME type with precision: "
								+ precision
								+ ", it only supports precision less than 3.");
			}
			// use int to represents Time, we only support millisecond when deserialization
			Schema time =
					LogicalTypes.timestampMillis().addToSchema(SchemaBuilder.builder().longType());
			return nullable ? nullableSchema(time) : time;
		case "DECIMAL":
			Schema decimal =
					LogicalTypes.decimal(10)
							.addToSchema(SchemaBuilder.builder().bytesType());
			return nullable ? nullableSchema(decimal) : decimal;
		case "ROW":
		case "MULTISET":
		case "MAP":
		case "ARRAY":
		case "RAW":
		case "TIMESTAMP_WITH_LOCAL_TIME_ZONE":
		default:
			throw new UnsupportedOperationException(
					"Unsupported to derive Schema for type: " + sqlType);
		}
	}
}
