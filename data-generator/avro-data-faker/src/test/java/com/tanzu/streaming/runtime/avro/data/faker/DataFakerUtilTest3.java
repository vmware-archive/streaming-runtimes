package com.tanzu.streaming.runtime.avro.data.faker;

import java.util.List;

import net.datafaker.Faker;
import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest3 {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext(List.of("song_id"));

		List<GenericData.Record> songs = DataFaker.generateRecords(
				DataFaker.resourceUriToAvroSchema("classpath:/avro/song.avsc"),
				100,
				sharedFieldValuesContext,
				SharedFieldValuesContext.Mode.PRODUCER,
				"song_id",
				System.currentTimeMillis());

		List<GenericData.Record> plays = DataFaker.generateRecords(
				DataFaker.resourceUriToAvroSchema("classpath:/avro/playsongs.avsc"),
				100,
				sharedFieldValuesContext,
				SharedFieldValuesContext.Mode.CONSUMER,
				null,
				System.currentTimeMillis());

		songs.forEach(System.out::println);
		System.out.println("---");
		plays.forEach(System.out::println);


		DataFaker.generateRecords(
				DataFaker.resourceUriToAvroSchema("classpath:/avro/iot-monitoring.yaml"),
				100,
				sharedFieldValuesContext,
				SharedFieldValuesContext.Mode.CONSUMER,
				null,
				System.currentTimeMillis()).forEach(System.out::println);
	}

}
