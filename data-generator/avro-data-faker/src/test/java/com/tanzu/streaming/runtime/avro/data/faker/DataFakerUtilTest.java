package com.tanzu.streaming.runtime.avro.data.faker;

import java.util.List;
import java.util.Random;

import com.tanzu.streaming.runtime.avro.data.faker.util.SharedFieldValuesContext;
import net.datafaker.Faker;
import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext(new Random());

		// Anomaly Detection records
		List<GenericData.Record> anomalyDetection = DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/anomaly.detection.yaml"),
				35,
				sharedFieldValuesContext,
				System.currentTimeMillis());
		anomalyDetection.forEach(System.out::println);

		// User records
		List<GenericData.Record> userRecords = DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/user1.avsc"),
				15,
				sharedFieldValuesContext,
				System.currentTimeMillis());


		// Click records
		List<GenericData.Record> clickRecords = DataGenerator.generateRecords(
//				DataFaker.resourceUriToAvroSchema("classpath:/avro/click.avsc"),
				DataGenerator.uriToSchema("classpath:/avro/click.yaml"),
				20,
				sharedFieldValuesContext,
				System.currentTimeMillis()); // (re)use the userId values from the user generation.


		userRecords.forEach(System.out::println);
		System.out.println("---");
		clickRecords.forEach(System.out::println);

		Faker faker = new Faker();
//		System.out.println(faker.address().fullAddress());
//		System.out.println(faker.music().genre());

//		System.out.println(faker.hitchhikersGuideToTheGalaxy().quote());
//		System.out.println(faker.expression("#{number.number_between '1','10'}"));
//		System.out.println(faker.expression("#{name.full_name}"));
//		System.out.println(faker.expression("#{options.option 'AA','BB','CC'}"));
//		System.out.println(faker.expression("#{date.future '365','DAYS'}"));
		System.out.println(faker.expression("#{finance.creditCard}"));
		for (int i = 0; i < 10; i++)
			System.out.println(faker.expression("#{business.credit_card_numbers}"));
		System.out.println(faker.expression("#{business.credit_card_types}"));
		System.out.println(faker.expression("#{business.credit_card_expiry_dates}"));

//		System.out.println(faker.finance().iban());

		List<GenericData.Record> songs = DataGenerator.generateRecords(
				DataGenerator.uriToSchema("classpath:/avro/song.avsc"),
				10,
				sharedFieldValuesContext,
				System.currentTimeMillis());

		songs.forEach(System.out::println);
	}

}
