package com.tanzu.streaming.runtime.data.generator;

import java.util.List;
import java.util.Random;

import com.tanzu.streaming.runtime.data.generator.context.SharedFieldValuesContext;
import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest2 {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext(new Random());

		// Anomaly Detection records
		List<GenericData.Record> anomalyDetection = DataUtil.toList(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/anomaly.detection.yaml"),
				20,
				sharedFieldValuesContext));

		List<GenericData.Record> anomalyDetection2 = DataUtil.toList(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/anomaly.detection.2.yaml"),
				10,
				sharedFieldValuesContext));

		anomalyDetection.forEach(System.out::println);
		System.out.println("---");
		anomalyDetection2.forEach(System.out::println);

	}

}
