package com.vmware.tanzu.streaming.runtime.throwaway;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.flink.sql.parser.ddl.SqlCreateTable;
import org.apache.flink.sql.parser.ddl.SqlTableColumn;
import org.apache.flink.sql.parser.impl.FlinkSqlParserImpl;

import org.springframework.stereotype.Component;

public class NaiveSqlToAvroSchemaConverter {

	private static SqlParser.Config SQL_PARSER_CONFIG = SqlParser.configBuilder()
			.setParserFactory(FlinkSqlParserImpl.FACTORY)
			.setConformance(SqlConformanceEnum.MYSQL_5)
			.setLex(Lex.MYSQL)
			.build();


	public Schema parse(String sql) {

		try {
			SqlParser parser = SqlParser.create(sql, SQL_PARSER_CONFIG);

			SqlCreateTable createTable = (SqlCreateTable) parser.parseStmt();

			SchemaBuilder.FieldAssembler<Schema> recordFieldBuilder = SchemaBuilder
					.record(createTable.getTableName().getSimple())
					.namespace("sql.generated." + createTable.getTableName().getSimple().toUpperCase())
					.fields();

			for (SqlNode c : createTable.getColumnList().getList()) {
				String columnName = ((SqlTableColumn.SqlRegularColumn) c).getName().getSimple();
				String columnType = ((SqlTableColumn.SqlRegularColumn) c).getType()
						.toSqlString(MysqlSqlDialect.DEFAULT).getSql();
				boolean isNullable = ((SqlTableColumn.SqlRegularColumn) c).getType().getNullable();
				Schema avroFieldType = convertToAvroSchema(columnType, isNullable);
				recordFieldBuilder.name(columnName).type(avroFieldType).noDefault();
			}

			return recordFieldBuilder.endRecord();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Schema convertToAvroSchema(String sqlType, boolean nullable) {
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

	private static Schema nullableSchema(Schema schema) {
		return schema.isNullable()
				? schema
				: Schema.createUnion(SchemaBuilder.builder().nullType(), schema);
	}

}
