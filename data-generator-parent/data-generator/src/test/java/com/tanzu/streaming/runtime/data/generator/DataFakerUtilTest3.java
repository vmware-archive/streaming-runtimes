package com.tanzu.streaming.runtime.data.generator;

import java.util.List;

import com.tanzu.streaming.runtime.data.generator.context.SharedFieldValuesContext;
import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest3 {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext();

		List<GenericData.Record> songs = DataUtil.toList(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/song.avsc"),
				100,
				sharedFieldValuesContext));

		List<GenericData.Record> plays = DataUtil.toList(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/playsongs.avsc"),
				100,
				sharedFieldValuesContext));

		songs.forEach(System.out::println);
		System.out.println("---");
		plays.forEach(System.out::println);


		DataUtil.toList(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/iot-monitoring.yaml"),
				100,
				sharedFieldValuesContext)).forEach(System.out::println);
	}

}
