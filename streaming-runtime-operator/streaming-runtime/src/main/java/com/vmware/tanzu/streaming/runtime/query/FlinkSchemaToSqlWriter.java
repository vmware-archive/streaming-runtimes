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
package com.vmware.tanzu.streaming.runtime.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.flink.table.utils.EncodingUtils;

import org.springframework.stereotype.Component;

/**
 * Helps to convert Flink Schema instance into CREATE TABLE DDL/SQL statement.
 */
@Component
public class FlinkSchemaToSqlWriter {

	/**
	 * Convert Flink Schema (plus table options) into executable CREATE TABLE DDL statement.
	 * @param schemaName Flink schema name used as Table name.
	 * @param flinkSchema Input Flink schema.
	 * @param tableOptions Input table options.
	 * @return Returns executable CREATE TABLE DDL statement compliant with the Flink Avro/UpserAvro connectors.
	 */
	public String toSql(String schemaName, org.apache.flink.table.api.Schema flinkSchema, Map<String, String> tableOptions) {
		List<String> allColumns = new ArrayList<>();

		allColumns.addAll(flinkSchema.getColumns().stream().map(this::printColumn)
				.collect(Collectors.toList()));
		allColumns.addAll(flinkSchema.getWatermarkSpecs().stream().map(this::printWatermark)
				.collect(Collectors.toList()));
		flinkSchema.getPrimaryKey().ifPresent(pk -> allColumns.add(this.printPrimaryKey(pk)));


		return String.format("CREATE TABLE %s (%n%s%n) WITH (%n%s %n)%n", schemaName,
				allColumns.stream().map(s -> "   " + s).collect(Collectors.joining(",\n")),
				this.printOptions(tableOptions));
	}

	private String printWatermark(org.apache.flink.table.api.Schema.UnresolvedWatermarkSpec watermarkSpec) {
		String expression = watermarkSpec.getWatermarkExpression().asSummaryString();
		return String.format(
				"WATERMARK FOR %s AS %s",
				EncodingUtils.escapeIdentifier(watermarkSpec.getColumnName()),
				expression.substring(1, expression.length() - 1));

	}

	private String printColumn(org.apache.flink.table.api.Schema.UnresolvedColumn column) {
		if (column instanceof org.apache.flink.table.api.Schema.UnresolvedMetadataColumn) {
			return printColumn((org.apache.flink.table.api.Schema.UnresolvedMetadataColumn) column);
		}
		else if (column instanceof org.apache.flink.table.api.Schema.UnresolvedComputedColumn) {
			return printColumn((org.apache.flink.table.api.Schema.UnresolvedComputedColumn) column);
		}
		else if (column instanceof org.apache.flink.table.api.Schema.UnresolvedPhysicalColumn) {
			return printColumn((org.apache.flink.table.api.Schema.UnresolvedPhysicalColumn) column);
		}
		else {
			final StringBuilder sb = new StringBuilder();
			sb.append(EncodingUtils.escapeIdentifier(column.getName()));
			return sb.toString();
		}
	}

	private String printColumn(org.apache.flink.table.api.Schema.UnresolvedMetadataColumn column) {
		final StringBuilder sb = new StringBuilder();
		sb.append(EncodingUtils.escapeIdentifier(column.getName()));
		sb.append(column.getDataType().toString());
		sb.append(" METADATA");
		if (column.getMetadataKey() != null) {
			sb.append(" FROM '");
			sb.append(EncodingUtils.escapeSingleQuotes(column.getMetadataKey()));
			sb.append("'");
		}
		if (column.isVirtual()) {
			sb.append(" VIRTUAL");
		}
		column.getComment().ifPresent(
				c -> {
					sb.append(" COMMENT '");
					sb.append(EncodingUtils.escapeSingleQuotes(c));
					sb.append("'");
				});
		return sb.toString();
	}

	private String printColumn(org.apache.flink.table.api.Schema.UnresolvedComputedColumn column) {
		String expressionStr = column.getExpression().asSummaryString();
		final StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s AS %s", EncodingUtils.escapeIdentifier(column.getName()), expressionStr.substring(1, expressionStr.length() - 1))); // Removes the outer [...] brackets
		column.getComment().ifPresent(
				c -> {
					sb.append(" COMMENT '");
					sb.append(EncodingUtils.escapeSingleQuotes(c));
					sb.append("'");
				});
		return sb.toString();
	}

	private String printColumn(org.apache.flink.table.api.Schema.UnresolvedPhysicalColumn column) {
		final StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s %s", EncodingUtils.escapeIdentifier(column.getName()), column.getDataType()
				.toString()));
		column.getComment().ifPresent(
				c -> {
					sb.append(" COMMENT '");
					sb.append(EncodingUtils.escapeSingleQuotes(c));
					sb.append("'");
				});
		return sb.toString();
	}

	private String printPrimaryKey(org.apache.flink.table.api.Schema.UnresolvedPrimaryKey primaryKey) {

		return String.format(
				"PRIMARY KEY (%s) NOT ENFORCED",
				primaryKey.getColumnNames().stream()
						.map(EncodingUtils::escapeIdentifier)
						.collect(Collectors.joining(", ")));

	}

	private String printOptions(Map<String, String> options) {
		return options.entrySet().stream()
				.map(entry -> String.format("  '%s' = '%s'",
						EncodingUtils.escapeSingleQuotes(entry.getKey()),
						EncodingUtils.escapeSingleQuotes(entry.getValue())))
				.collect(Collectors.joining(String.format(",%n")));
	}
}
