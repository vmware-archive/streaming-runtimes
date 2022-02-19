package com.tanzu.streaming.runtime.avro.data.faker;

import java.util.List;

import com.tanzu.streaming.runtime.avro.data.faker.util.SharedFieldValuesContext;
import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest3 {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext();

		List<GenericData.Record> songs = DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/song.avsc"),
				100,
				sharedFieldValuesContext,
				System.currentTimeMillis());

		List<GenericData.Record> plays = DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/playsongs.avsc"),
				100,
				sharedFieldValuesContext,
				System.currentTimeMillis());

		songs.forEach(System.out::println);
		System.out.println("---");
		plays.forEach(System.out::println);


		DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/iot-monitoring.yaml"),
				100,
				sharedFieldValuesContext,
				System.currentTimeMillis()).forEach(System.out::println);
	}

}
