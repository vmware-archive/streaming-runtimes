package com.tanzu.streaming.runtime.data.generator;

import java.util.List;

import com.tanzu.streaming.runtime.data.generator.context.SharedFieldValuesContext;
import net.datafaker.Faker;
import org.apache.avro.generic.GenericData;

public class DataFakerUtilTest4 {


	public static void main(String[] args) {

		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext();

		// User records
		List<GenericData.Record> userRecords = DataUtil.toList(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/user.avsc"),
				15,
				sharedFieldValuesContext));


		// Click records
		List<GenericData.Record> clickRecords = DataUtil.toList(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/click.yaml"),
				20,
				sharedFieldValuesContext)); // (re)use the userId values from the user generation.


		userRecords.forEach(System.out::println);
		System.out.println("---");
		clickRecords.forEach(System.out::println);

		Faker faker = new Faker();
		System.out.println(faker.expression("#{business.credit_card_types}"));
		System.out.println(faker.expression("#{business.credit_card_expiry_dates}"));

		String usersSchema = "{\n"
				+ "  \"namespace\": \"my.clickstream.app\",\n"
				+ "  \"type\": \"record\",\n"
				+ "  \"name\": \"User\",\n"
				+ "  \"fields\": [\n"
				+ "    {\n"
				+ "      \"name\": \"user_id\",\n"
				+ "      \"type\": \"string\"\n"
				+ "    },\n"
				+ "    {\n"
				+ "      \"name\": \"name\",\n"
				+ "      \"type\": \"string\"\n"
				+ "    },\n"
				+ "    {\n"
				+ "      \"name\": \"department\",\n"
				+ "      \"type\": \"string\"\n"
				+ "    },\n"
				+ "    {\n"
				+ "      \"name\": \"industry\",\n"
				+ "      \"type\": \"string\"\n"
				+ "    }\n"
				+ "  ]\n"
				+ "}";

		List<GenericData.Record> users = DataUtil.toList(new DataGenerator(
				DataUtil.contentToSchema(usersSchema), 3));
		users.forEach(System.out::println);

		String usersSchemaWithFaker = "{\n"
				+ "  \"namespace\": \"my.clickstream\",\n"
				+ "  \"type\": \"record\",\n"
				+ "  \"name\": \"User\",\n"
				+ "  \"fields\": [\n"
				+ "    {\n"
				+ "      \"name\": \"user_id\",\n"
				+ "      \"type\": \"string\",\n"
				+ "      \"doc\": \"#{id_number.valid}\"\n"
				+ "    },\n"
				+ "    {\n"
				+ "      \"name\": \"name\",\n"
				+ "      \"type\": \"string\",\n"
				+ "      \"doc\": \"#{name.fullName}\"\n"
				+ "    },\n"
				+ "    {\n"
				+ "      \"name\": \"department\",\n"
				+ "      \"type\": \"string\",\n"
				+ "      \"doc\": \"#{commerce.department}\"\n"
				+ "    },\n"
				+ "    {\n"
				+ "      \"name\": \"industry\",\n"
				+ "      \"type\": \"string\",\n"
				+ "      \"doc\": \"#{company.industry}\"\n"
				+ "    }\n"
				+ "  ]\n"
				+ "}";

		DataUtil.toList(new DataGenerator(DataUtil.contentToSchema(usersSchemaWithFaker), 3))
				.forEach(System.out::println);

	}

}
