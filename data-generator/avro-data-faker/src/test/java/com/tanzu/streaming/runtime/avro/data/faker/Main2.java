package com.tanzu.streaming.runtime.avro.data.faker;

import com.tanzu.streaming.runtime.avro.data.faker.util.SharedFieldValuesContext;
import com.tanzu.streaming.runtime.avro.data.faker.util.SpELTemplateParserContext;
import net.datafaker.Faker;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class Main2 {
	public static void main(String[] args) {
		Faker faker = new Faker();
		StandardEvaluationContext spelContext = new StandardEvaluationContext();
		spelContext.setVariable("faker", faker);
		SpELTemplateParserContext spelTemplateContext = new SpELTemplateParserContext();
		SpelExpressionParser spelParser = new SpelExpressionParser();

		String bla = "keyField=card_number,boza=#{name.fullName} [[T(System).currentTimeMillis()]],koza=[[T(System).currentTimeMillis()]]";

		SharedFieldValuesContext sharedContext = new SharedFieldValuesContext();

		sharedContext.addValue("user_id", "value1");
		sharedContext.addValue("user_id", "value2");

		spelContext.setVariable("shared", sharedContext);

		System.out.println(spelParser.parseExpression("[[#shared.getRandomValue('user_id')]]",
				spelTemplateContext).getValue(spelContext, String.class));
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
