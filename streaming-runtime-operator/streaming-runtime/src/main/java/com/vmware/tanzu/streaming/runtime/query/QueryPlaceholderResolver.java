package com.vmware.tanzu.streaming.runtime.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;

public class QueryPlaceholderResolver {

	private static final Logger LOG = LoggerFactory.getLogger(QueryPlaceholderResolver.class);

	private static final Pattern IN_SQL_STREAM_NAME_PATTERN = Pattern.compile("\\[\\[STREAM:(\\S*)\\]\\]", Pattern.CASE_INSENSITIVE);

	/**
	 * Retrieves the stream references placeholders in the quiereis.
	 * @param sqlQueries Processor SQL queries that may contain stream reference placeholders such as [[STREAM:stream-name]]
	 * @return returns a map of the placeholder (as it appears in the query) and the name of the stream it refers to:
	 *  [[STREAM:stream-name]] -> stream-name
	 */
	public static Map<String, String> extractPlaceholders(List<String> sqlQueries) {

		Map<String, String> placeholderToStreamMap = new HashMap<>();

		if (!CollectionUtils.isEmpty(sqlQueries)) {
			for (String sql : sqlQueries) {
				Matcher matcher = IN_SQL_STREAM_NAME_PATTERN.matcher(sql);

				for (MatchResult mr : matcher.results().collect(Collectors.toList())) {
					String placeholder = mr.group();
					String streamName = mr.group(1);
					LOG.info(placeholder + " -> " + streamName);
					placeholderToStreamMap.put(placeholder, streamName);
				}
			}
		}
		return placeholderToStreamMap;
	}

	/**
	 * Resolves the stream-reference placeholders by replacing them with the (table) names
	 * in the provided substitution map.
	 * @param sqlQueries Sql queries with stream-ref placeholders.
	 * @param placeholderToTableNames placeholder substitution map containing Table name for every placeholder.
	 * @return Returns executable queries.
	 */
	public static List<String> resolveQueries(List<String> sqlQueries, Map<String, String> placeholderToTableNames) {
		return (!CollectionUtils.isEmpty(sqlQueries)) ?
				sqlQueries.stream()
						.map(sql -> {
							String outSql = sql;
							for (Map.Entry<String, String> e : placeholderToTableNames.entrySet()) {
								String placeholder = e.getKey();
								String tableName = e.getValue();
								outSql = outSql.replace(placeholder, tableName);
							}
							return outSql;
						})
						.collect(Collectors.toList())
				: new ArrayList<>();
	}

}
