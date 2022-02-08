package com.vmware.tanzu.streaming.runtime.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vmware.tanzu.streaming.models.V1alpha1StreamSpecDataSchemaContextMetadataFields;
import com.vmware.tanzu.streaming.runtime.dataschema.DataSchemaAvroConverter;
import com.vmware.tanzu.streaming.runtime.dataschema.DataSchemaProcessingContext;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.flink.formats.avro.typeutils.AvroSchemaConverter;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.expressions.SqlCallExpression;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class DataSchemaToFlinkDdlConverter {

	private static final Logger LOG = LoggerFactory.getLogger(DataSchemaToFlinkDdlConverter.class);

	public static final String FLINK_OPTIONS_PREFIX = "ddl.";
	public static final String AVRO = "avro";
	public static final String JSON = "json";

	private final FlinkSchemaToSqlWriter flinkSchemaToSqlWriter;
	private final HashMap<String, DataSchemaAvroConverter> dataSchemaAvroConverters;

	public DataSchemaToFlinkDdlConverter(FlinkSchemaToSqlWriter flinkSchemaToSqlWriter,
			DataSchemaAvroConverter[] schemaAvroConverters) {

		this.flinkSchemaToSqlWriter = flinkSchemaToSqlWriter;

		this.dataSchemaAvroConverters = new HashMap<>();
		Stream.of(schemaAvroConverters).forEach(converter -> {
			this.dataSchemaAvroConverters.put(converter.getSupportedDataSchemaType(), converter);
		});
	}

	/**
	 * Holder of the Table Name and CREATE TABLE DDL.
	 */
	public static class TableDdlInfo {

		private final String tableName;
		private final String tableDdl;

		public TableDdlInfo(String tableName, String tableDdl) {
			this.tableDdl = tableDdl;
			this.tableName = tableName;
		}

		public String getTableDdl() {
			return tableDdl;
		}

		public String getTableName() {
			return tableName;
		}
	}

	/**
	 * Convert a Stream data-schema into executable Flink CREATE TABLE DDL with related time-attributes, metadata and
	 * options.
	 * @param context Single stream CD with data schema for which DDL is generated.
	 * @return Returns Stream data-schema into executable Flink CREATE TABLE DDL with related time-attributes, metadata and options.
	 */
	public TableDdlInfo createFlinkTableDdl(DataSchemaProcessingContext context) {
		this.addFlinkTableOptions(context);
		return convertSchemaDataContextToFlinkTableDdl(context);
	}


	/**
	 * And opinionated Flink options inferred form Stream's status.
	 *
	 * @param context Data schema context
	 */
	private void addFlinkTableOptions(DataSchemaProcessingContext context) {

		Map<String, String> customOptions = new HashMap<>();
		// If the value format is not set, default to avro.
		String ddlValueFormat = Optional.ofNullable(context.getOptions().get("ddl.value.format")).orElse(AVRO);
		customOptions.put("ddl.value.format", ddlValueFormat);

		String connector = Optional.ofNullable(context.getOptions().get("ddl.connector"))
				.orElse(context.getStreamProtocol());
		customOptions.put("ddl.connector", connector);
		customOptions.put("ddl.topic", context.getStreamName());
		customOptions.put("ddl.properties.bootstrap.servers",
				context.getOptions().get(DataSchemaProcessingContext.STREAM_STATUS_SERVER_PREFIX + "brokers"));

		// Add the following, additional, key and value configurations in case of Avro or Json value formats
		if (AVRO.equalsIgnoreCase(ddlValueFormat)) { //AVRO
			customOptions.put("ddl.key.format", "raw");
			customOptions.put("ddl.key.fields", "the_kafka_key");
			customOptions.put("ddl.value.fields-include", "EXCEPT_KEY");
			String schemaRegistryUrl = context.getOptions()
					.get(DataSchemaProcessingContext.STREAM_STATUS_SERVER_PREFIX + "schemaRegistry");
			if (StringUtils.hasText(schemaRegistryUrl)) {
				customOptions.put("ddl.value.format", "avro-confluent");
				customOptions.put("ddl.value.avro-confluent.url", schemaRegistryUrl);
			}
		}
		else if (JSON.equalsIgnoreCase(ddlValueFormat)) { //JSON
			customOptions.put("ddl.key.format", JSON);
			customOptions.put("ddl.value.format", JSON);
			customOptions.put("ddl.key.json.ignore-parse-errors", "true");
			customOptions.put("ddl.value.json.fail-on-missing-field", "false");
			customOptions.put("ddl.value.fields-include", "ALL");
		}

		// Update/Extend the context options with flink specific configurations.
		context.getOptions().putAll(customOptions);
	}

	/**
	 * Converts the dataSchema and related options into executable Flink Create Table DDL statement.
	 * @return The TableDdlInfo contains the CREATE TABLE DDL statement and the related Table name.
	 *
	 */
	private TableDdlInfo convertSchemaDataContextToFlinkTableDdl(DataSchemaProcessingContext context) {

		// Create Avro Schema from the CR's data schema (inline or meta schemas)
		DataSchemaAvroConverter dataSchemaAvroConverter = this.dataSchemaAvroConverters.get(context.getDataSchemaType());
		Schema schema = dataSchemaAvroConverter.toAvro(context);
		String avroSchema = schema.toString(true);
		String schemaName = schema.getName();

		// Turn the Avro schema into Flink DataType
		DataType dataType = AvroSchemaConverter.convertToDataType(avroSchema);

		// Helper class that allow to check if a column name is already present in the target table.
		// The column can either exist as part of the initial avro/datatype schema or added as time/metadata attribute.
		ColumnExistenceChecker columnExistenceChecker = new ColumnExistenceChecker(dataType);

		// BEGIN FLINK TABLE BUILDING
		org.apache.flink.table.api.Schema.Builder flinkSchemaBuilder =
				org.apache.flink.table.api.Schema.newBuilder().fromRowDataType(dataType);

		// Retrieve schema's options and remove the prefix (e.g. make them Flink align).
		Map<String, String> tableOptions = context.getOptions().entrySet().stream()
				.filter(e -> e.getKey().startsWith(FLINK_OPTIONS_PREFIX))
				.collect(Collectors.toMap(e -> e.getKey()
						.substring(FLINK_OPTIONS_PREFIX.length()), Map.Entry::getValue));

		// Add column for the raw key.fields.
		// TODO this is an opinionated assumption that for (key.format == "raw") && (not empty key.fields) condition
		//  we always have to add the key.fields as table fields (if not present).
		//  This might be just kafka specific.
		if ("raw".equalsIgnoreCase(tableOptions.get("key.format")) && tableOptions.containsKey("key.fields")) {
			String rawKeyFormatColumnNames = tableOptions.get("key.fields");
			for (String columnName : rawKeyFormatColumnNames.split(",")) {
				if (!columnExistenceChecker.isColumnExist(columnName)) {
					flinkSchemaBuilder.column(columnName, DataTypes.STRING().notNull());
					columnExistenceChecker.markColumnNameAsExisting(columnName);
				}
			}
		}

		// METADATA FIELDS: Add the metadata fields as table columns.
		for (V1alpha1StreamSpecDataSchemaContextMetadataFields metadataField : context.getMetadataFields().values()) {

			normalizeFieldType(metadataField);

			String metadataFrom = metadataField.getMetadata().getFrom();

			// if both the field name and the metadataFrom name are the same skip the second.
			if (metadataField.getName().equalsIgnoreCase(metadataField.getMetadata().getFrom())) {
				metadataFrom = null;
			}

			flinkSchemaBuilder.columnByMetadata(
					metadataField.getName(),
					dataTypeOf(metadataField),
					metadataFrom,
					Optional.ofNullable(metadataField.getMetadata().getReadonly()).orElse(false));

			columnExistenceChecker.markColumnNameAsExisting(metadataField.getName());
		}

		// PRIMARY KEY: If defined convert the primary key into table primary-key constrain.
		List<String> primaryKey = context.getDataSchemaContext().getPrimaryKey();
		if (!CollectionUtils.isEmpty(primaryKey)) {
			flinkSchemaBuilder.primaryKey(primaryKey);
		}

		// TIME ATTRIBUTES: Convert the collected time attributes into WATERMARKS or PROCTIME columns.
		if (!CollectionUtils.isEmpty(context.getTimeAttributes())) {
			for (String timeAttributeFieldName : context.getTimeAttributes().keySet()) {

				// Not-Empty watermark stands for EVENT-TIME time attribute.
				// Empty watermark stands for PROCTIME time attribute.
				String watermark = context.getTimeAttributes().get(timeAttributeFieldName);

				if (columnExistenceChecker.isColumnExist(timeAttributeFieldName)) {
					if (StringUtils.hasText(watermark)) {// Event Time
						flinkSchemaBuilder.watermark(timeAttributeFieldName, new SqlCallExpression(watermark));
					}
					else {
						// TODO check that the existing column already has a PROCTIME time
					}
				}
				else { // add new column
					if (!StringUtils.hasText(watermark)) { // PROC TIME
						flinkSchemaBuilder.columnByExpression(timeAttributeFieldName, new SqlCallExpression("PROCTIME()"));
					}
					else {
						throw new RuntimeException(
								String.format("Missing table [%s] column for Event Time attribute: %s, %s",
										schemaName, timeAttributeFieldName, watermark));
					}
				}
			}
		}

		// END FLINK TABLE BUILDING
		org.apache.flink.table.api.Schema flinkSchema = flinkSchemaBuilder.build();

		return new TableDdlInfo(schemaName,
				this.flinkSchemaToSqlWriter.toSql(schemaName, flinkSchema, tableOptions));
	}

	/**
	 * Converts field's type into Flink DataType.
	 */
	private DataType dataTypeOf(V1alpha1StreamSpecDataSchemaContextMetadataFields field) {
		Schema fieldTypeSchema = Schema.create(Schema.Type.valueOf(field.getType().toUpperCase()));
		if (StringUtils.hasText(field.getLogicalType())) {
			fieldTypeSchema = new LogicalType(field.getLogicalType()).addToSchema(fieldTypeSchema); // add logical type
		}
		if (field.getOptional() != null && field.getOptional()) {
			fieldTypeSchema = Schema.createUnion(Schema.create(Schema.Type.NULL), fieldTypeSchema); // add ["null", "type"] union
		}
		return AvroSchemaConverter.convertToDataType(fieldTypeSchema.toString(false));
	}

	/**
	 * Splits Field's shortcut type-format, such as long_timestamp-millis, into type (long)
	 * and logicalType (timestamp-millis) parts. If the type-format is not shortcut does nothing.
	 */
	private void normalizeFieldType(V1alpha1StreamSpecDataSchemaContextMetadataFields field) {
		if (field.getType().split("_").length > 1) {
			String[] typeAndLogicalType = field.getType().split("_");
			field.setLogicalType(typeAndLogicalType[1]);
			field.setType(typeAndLogicalType[0]);
		}
	}

	/**
	 * Helper class that allow to check if a column name is already present in the target table.
	 * The column can either exist as part of the initial avro/datatype schema or added as time/metadata attribute.
	 */
	private static class ColumnExistenceChecker {

		private final Map<String, String> newAddedFields;

		ColumnExistenceChecker(DataType dataType) {
			this.newAddedFields = new ConcurrentHashMap<>();
			((RowType) dataType.getLogicalType()).getFields()
					.forEach(rt -> newAddedFields.put(rt.getName(), ""));
		}

		public void markColumnNameAsExisting(String fieldName) {
			this.newAddedFields.put(fieldName, "");
		}

		public boolean isColumnExist(String fieldName) {
			return this.newAddedFields.containsKey(fieldName);
		}
	}
}
