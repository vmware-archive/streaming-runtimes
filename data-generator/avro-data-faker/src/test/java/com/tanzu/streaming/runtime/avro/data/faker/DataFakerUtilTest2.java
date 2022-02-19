package com.tanzu.streaming.runtime.avro.data.faker;

import java.util.List;
import java.util.Random;

import com.tanzu.streaming.runtime.avro.data.faker.util.SharedFieldValuesContext;
import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest2 {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext(new Random());

		// Anomaly Detection records
		List<GenericData.Record> anomalyDetection = DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/anomaly.detection.yaml"),
				20,
				sharedFieldValuesContext,
				System.currentTimeMillis());

		List<GenericData.Record> anomalyDetection2 = DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/anomaly.detection.2.yaml"),
				10,
				sharedFieldValuesContext,
				System.currentTimeMillis());

		anomalyDetection.forEach(System.out::println);
		System.out.println("---");
		anomalyDetection2.forEach(System.out::println);

	}

}
