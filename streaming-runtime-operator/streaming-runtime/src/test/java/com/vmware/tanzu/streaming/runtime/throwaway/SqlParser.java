package com.vmware.tanzu.streaming.runtime.throwaway;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;

import org.springframework.core.io.DefaultResourceLoader;

public class SqlParser {
	private static final Pattern IN_SQL_STREAM_NAME_PATTERN = Pattern.compile("\\[\\[STREAM:(\\S*)\\]\\]", Pattern.CASE_INSENSITIVE);

	public static void main(String[] args) {
//				- INSERT INTO TopKSongsPerGenre
//		SELECT window_start, window_end, song_id, name, genre, play_count
//		FROM (
//				SELECT *,
//				ROW_NUMBER() OVER (PARTITION BY window_start, window_end, genre ORDER BY play_count DESC) AS row_num
//				FROM (
//						SELECT window_start, window_end, song_id, name, genre, COUNT(*) AS play_count
//				FROM TABLE(
//						TUMBLE(TABLE SongPlays, DESCRIPTOR(event_time), INTERVAL '60' SECONDS))
//		GROUP BY window_start, window_end, song_id, name, genre
//              )
//            ) WHERE row_num <= 3

		String sql = "INSERT INTO [[STREAM:kafka-stream-songplays]] \n" +
				"SELECT Plays.song_id, Songs.album, Songs.artist, Songs.name, Songs.genre, Plays.duration, Plays.event_time \n" +
				"FROM(SELECT * FROM [[STREAM:kafka-stream-playevents]] WHERE duration >= 30000) AS Plays \n" +
				"INNER JOIN [[STREAM:kafka-stream-song]] ON Plays.song_id = Songs.id \n";

		System.out.println(sql + "\n");
		System.out.println(substituteStreamNames(sql));

		String sql2 = "INSERT INTO [[STREAM:kafka-stream-topksongspergenre]] \n" +
				"SELECT window_start, window_end, song_id, name, genre, play_count \n" +
				"FROM ( \n" +
				"		SELECT *, \n" +
				"		ROW_NUMBER() OVER (PARTITION BY window_start, window_end, genre ORDER BY play_count DESC) AS row_num \n" +
				"		FROM ( \n" +
				"				SELECT window_start, window_end, song_id, name, genre, COUNT(*) AS play_count \n" +
				"		FROM TABLE( \n" +
				"				TUMBLE(TABLE [[STREAM:kafka-stream-songplays]], DESCRIPTOR(event_time), INTERVAL '60' SECONDS)) \n" +
				"       GROUP BY window_start, window_end, song_id, name, genre \n" +
				"       ) \n" +
				"    ) WHERE row_num <= 3 \n";

		System.out.println(sql2 + "\n");
		System.out.println(substituteStreamNames(sql2));

	}

	public static String substituteStreamNames(String inSql) {
		Matcher matcher = IN_SQL_STREAM_NAME_PATTERN.matcher(inSql);
		Map<String, String> map = matcher.results()
				.collect(Collectors.toMap(MatchResult::group, mr -> toTableName(mr.group(1))));
		//System.out.println(map);

		String outSql = inSql;
		for (Map.Entry<String, String> e : map.entrySet()) {
			outSql = outSql.replace(e.getKey(), e.getValue());
		}

		return outSql;
	}

	static Map<String, String> streamToTableMap = new HashMap<>();
	static String[] streamYamlPaths = new String[] {
			"file:./streaming-runtime/src/test/java/com/vmware/tanzu/streaming/runtime/throwaway/SongStream.yaml",
			"file:./streaming-runtime/src/test/java/com/vmware/tanzu/streaming/runtime/throwaway/PlayEventsStream.yaml",
			"file:./streaming-runtime/src/test/java/com/vmware/tanzu/streaming/runtime/throwaway/SongPlaysStream.yaml",
			"file:./streaming-runtime/src/test/java/com/vmware/tanzu/streaming/runtime/throwaway/TopKSongsPerGenreStream.yaml"};

	static {
		for (String yp : streamYamlPaths) {
			try {
				V1alpha1Stream stream = toV1alpha1Stream(yp);
				streamToTableMap.put(stream.getMetadata().getName(),
						stream.getSpec().getDataSchemaContext().getSchema().getName());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String toTableName(String streamName) {
		return "'" + streamToTableMap.get(streamName) + "'";
	}

	public static V1alpha1Stream toV1alpha1Stream(String streamYamlResourceUri) throws IOException {
		ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
		yamlReader.registerModule(new JavaTimeModule());
		InputStream yaml = new DefaultResourceLoader().getResource(streamYamlResourceUri).getInputStream();
		V1alpha1Stream stream = yamlReader.readValue(yaml, V1alpha1Stream.class);
		return stream;
	}

}
