package com.tanzu.streaming.runtime.sqlaggregator;

import java.util.List;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlDdl;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserImplFactory;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.formats.avro.typeutils.AvroSchemaConverter;
import org.apache.flink.sql.parser.ddl.SqlCreateTable;
import org.apache.flink.sql.parser.impl.FlinkSqlParserImpl;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.internal.TableEnvironmentImpl;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.ResolvedCatalogTable;
import org.apache.flink.table.operations.Operation;
import org.apache.flink.table.operations.ddl.CreateTableOperation;
import org.apache.flink.table.types.DataType;

public class Main {
	public static void main(String[] args) throws SqlParseException {
		Configuration strConf = new Configuration();
		strConf.setInteger(RestOptions.PORT, 8089); // Flink UI port.
		strConf.setString(RestOptions.BIND_PORT, "8088-8090");
		strConf.set(TaskManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("1728", MemorySize.MemoryUnit.MEGA_BYTES));
		strConf.set(JobManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("1600", MemorySize.MemoryUnit.MEGA_BYTES));

		StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(1, strConf);
		env.getConfig().setMaxParallelism(4);
		StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

		String playEventsDdl = "CREATE TABLE PlayEvents (\n"
				+ "  `song_id` BIGINT NOT NULL,\n"
				+ "  `duration` BIGINT,\n"
				+ "  `event_time` TIMESTAMP(3) METADATA FROM 'timestamp',\n"
				+ "  `the_kafka_key` STRING,\n"
				+ "  WATERMARK FOR `event_time` AS `event_time` - INTERVAL '30' SECONDS\n"
				+ ")\n";

		String sql2 = "CREATE TABLE TopKSongsPerGenre (\n"
				+ "  `window_start` TIMESTAMP(3) NOT NULL,\n"
				+ "  `window_end` TIMESTAMP(3) NOT NULL,\n"
				+ "  `song_id` BIGINT NOT NULL,\n"
				+ "  `name` STRING NOT NULL,\n"
				+ "  `genre` STRING NOT NULL,\n"
				+ "  `song_play_count` BIGINT,\n"
				+ "  PRIMARY KEY (`window_start`, `window_end`, `song_id`, `genre`) NOT ENFORCED\n"
				+ ") WITH (\n"
				+ "  'connector' = 'upsert-kafka',\n"
				+ "  'topic' = 'kafka-stream-topk-songs-per-genre',\n"
				+ "  'properties.bootstrap.servers' = 'kafka.default.svc.cluster.local:9092',\n"
				+ "  'key.format' = 'json',\n"
				+ "  'value.format' = 'json',\n"
				+ "  'key.json.ignore-parse-errors' = 'true',\n"
				+ "  'value.json.fail-on-missing-field' = 'false',\n"
				+ "  'value.fields-include' = 'ALL',\n"
				+ "  'properties.allow.auto.create.topics' = 'true'\n"
				+ ")";

		String songsDdl = "CREATE TABLE Songs (\n"
				+ "  `id` BIGINT NOT NULL,\n"
				+ "  `name` STRING NOT NULL,\n"
				+ "  `album` STRING,\n"
				+ "  `artist` STRING,\n"
				+ "  `genre` STRING NOT NULL,\n"
				+ "  `proctime` AS PROCTIME(),\n"
				+ "  `the_kafka_key` STRING\n"
				+ ") WITH (\n"
				+ "  'connector' = 'kafka',\n"
				+ "  'topic' = 'kafka-stream-songs',\n"
				+ "  'properties.bootstrap.servers' = 'kafka.default.svc.cluster.local:9092',\n"
				+ "  'key.format' = 'raw',\n"
				+ "  'key.fields' = 'the_kafka_key',\n"
				+ "  'value.fields-include' = 'EXCEPT_KEY',\n"
				+ "  'scan.startup.mode' = 'earliest-offset',\n"
				+ "  'value.format' = 'avro-confluent',\n"
				+ "  'value.avro-confluent.url' = 'http://s-registry.default.svc.cluster.local:8081',\n"
				+ "  'properties.group.id' = 'testGroup'\n"
				+ ")";

		// BOUNDED datasource:
		// - NO METADATA
		// - NO WATERMARKS
		String jdbcTable = "CREATE TABLE MyUserTable (\n"
				+ "  `id` BIGINT,\n"
				+ "  `name` STRING,\n"
				+ "  `age` INT,\n"
				+ "  `status` BOOLEAN,\n"
				+ "  PRIMARY KEY (`id`) NOT ENFORCED\n"
				+ ") WITH (\n"
				+ "   'connector' = 'jdbc',\n"
				+ "   'url' = 'jdbc:mysql://localhost:3306/mydatabase',\n"
				+ "   'table-name' = 'users'\n"
				+ ")";


		String srGenerated = "CREATE TABLE SongPlays (\n"
				+ "   `song_id` BIGINT NOT NULL,\n"
				+ "   `album` STRING,\n"
				+ "   `artist` STRING,\n"
				+ "   `name` STRING,\n"
				+ "   `genre` STRING NOT NULL,\n"
				+ "   `duration` BIGINT,\n"
				+ "   `event_time5` TIMESTAMP(3) NOT NULL,\n"
				+ "   `event_time3`TIMESTAMP(3) NOT NULL METADATA FROM 'timestamp' VIRTUAL,\n"
				+ "   `event_time2`TIMESTAMP(3) NOT NULL METADATA FROM 'timestamp',\n"
				+ "   `event_time4` AS PROCTIME(),\n"
				+ "   `event_time` AS PROCTIME(),\n"
				+ "   WATERMARK FOR `event_time5` AS `event_time5` - INTERVAL '6' SECONDS,\n"
				+ "   PRIMARY KEY (`song_id`, `name`) NOT ENFORCED\n"
				+ ") WITH (\n"
				+ "  'value.format' = 'json',\n"
				+ "  'properties.group.id' = 'testGroup3',\n"
				+ "  'properties.allow.auto.create.topics' = 'true',\n"
				+ "  'key.fields' = 'song_id',\n"
				+ "  'scan.startup.mode' = 'earliest-offset' \n"
				+ ")";



		SqlParser.Config sqlParserConfig = SqlParser.configBuilder()
				.setParserFactory(FlinkSqlParserImpl.FACTORY)
				.setConformance(SqlConformanceEnum.MYSQL_5)
				.setLex(Lex.MYSQL)
				.build();

		SqlParser parser = SqlParser.create(srGenerated, sqlParserConfig);

		SqlCreateTable createTable = (SqlCreateTable) parser.parseStmt();
		List<Operation> result = ((TableEnvironmentImpl) tableEnv).getParser().parse(srGenerated);
		CreateTableOperation createOp = (CreateTableOperation) result.get(0);
		System.out.println(createOp.getTableIdentifier());
		System.out.println();
		CatalogTable table = createOp.getCatalogTable();
		Schema schema = table.getUnresolvedSchema();
		List<Schema.UnresolvedWatermarkSpec> wms = schema.getWatermarkSpecs();
		System.out.println(table.getUnresolvedSchema());

		ResolvedCatalogTable resolvedTable = ((TableEnvironmentImpl) tableEnv).getCatalogManager()
				.resolveCatalogTable(table);

		System.out.println(resolvedTable.getResolvedSchema());

		DataType dataType = resolvedTable.getResolvedSchema().toPhysicalRowDataType();

		System.out.println(dataType);

		org.apache.avro.Schema avroSchema = AvroSchemaConverter.convertToSchema(dataType.getLogicalType(), "myname");
		String avroSchemaString = avroSchema.toString(true);

		System.out.println(avroSchemaString);


	}
}
