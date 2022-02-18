package com.tanzu.streaming.runtime.avro.data.faker;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.avro.generic.GenericData;

public class Main2 {
	public static void main(String[] args) throws JsonProcessingException {
		List<GenericData.Record> userRecords = DataFaker.generateRecords(
				DataFaker.uriToAvroSchema("classpath:/avro/iot-monitoring.yaml"),
				15,
				null,
				null,
				System.currentTimeMillis());

		userRecords.forEach(System.out::println);


		System.out.println(DataFaker.toJsonArray(userRecords));
		System.out.println("***");
		System.out.println(DataFaker.toYamlArray(userRecords));

//		Faker faker = new Faker();
//		StandardEvaluationContext spelContext = new StandardEvaluationContext();
//		spelContext.setVariable("faker", faker);
//		SpELTemplateParserContext spelTemplateContext = new SpELTemplateParserContext();
//		SpelExpressionParser spelParser = new SpelExpressionParser();
//
//		String bla = "keyField=card_number,boza=#{name.fullName} [[T(System).currentTimeMillis()]],koza=[[T(System).currentTimeMillis()]]";
//
//		String[] pairs = bla.split(",");
//		for (String pair : pairs) {
//			String[] keyValue = pair.split("=");
//			String unresolvedKey = keyValue[0];
//			String unresolvedValue = keyValue[1];
//
//			String spELResolvedKey = spelParser.parseExpression(unresolvedKey, spelTemplateContext).getValue(spelContext, String.class);
//			String resolvedKey = faker.expression(spELResolvedKey);
//
//			String spELResolvedValue = spelParser.parseExpression(unresolvedValue, spelTemplateContext).getValue(spelContext, String.class);
//			String resolvedValue = faker.expression(spELResolvedValue);
//
//			System.out.println(String.format("%s=%s", resolvedKey, resolvedValue));
//			spelContext.setVariable(resolvedKey, resolvedValue);
//
//			System.out.println(spelParser.parseExpression("[[#boza]]", spelTemplateContext).getValue(spelContext, String.class));
//		}

	}
}
