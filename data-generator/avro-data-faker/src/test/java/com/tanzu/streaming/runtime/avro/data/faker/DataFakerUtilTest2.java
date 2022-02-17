package com.tanzu.streaming.runtime.avro.data.faker;

import java.util.List;

import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest2 {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext(List.of("card_number"));

		// Anomaly Detection records
		List<GenericData.Record> anomalyDetection = DataFaker.generateRecords(
				DataFaker.uriToAvroSchema("classpath:/avro/anomaly.detection.yaml"),
				20,
				sharedFieldValuesContext,
				SharedFieldValuesContext.Mode.PRODUCER,
				"card_number",
				System.currentTimeMillis());


		List<GenericData.Record> anomalyDetection2 = DataFaker.generateRecords(
				DataFaker.uriToAvroSchema("classpath:/avro/anomaly.detection.yaml"),
				10,
				sharedFieldValuesContext,
				SharedFieldValuesContext.Mode.CONSUMER,
				"card_number",
				System.currentTimeMillis());

		anomalyDetection.forEach(System.out::println);
		System.out.println("---");
		anomalyDetection2.forEach(System.out::println);

	}

}
