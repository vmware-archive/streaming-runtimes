package com.tanzu.streaming.runtime.avro.data.faker;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class Main {

	SpelExpressionParser spelParser = new SpelExpressionParser();

	SpELTemplateParserContext temParserCtx = new SpELTemplateParserContext();

	public static void main(String[] args) throws Exception {
		new Main().bla2();
	}

	public void bla2() {

		Faker faker = new Faker();
		System.out.println(faker.expression("[[test]]"));


		Map<String, String> myMap = new HashMap<>();
		myMap.put("foo", "bar");



		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("Box: [[['foo']]]", new SpELTemplateParserContext());
		//Expression exp = parser.parseExpression("'Hello [[myMap['foo']]]'", new TemplateParserContext());
		//Expression exp = parser.parseExpression("'Hello [[new net.datafaker.Faker().date().birthday()]]'", new TemplateParserContext());

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new MapAccessor());

		System.out.println(exp.getValue(context, myMap));

		bla3();

	}

	public void bla3() {
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("faker", new Faker());
		System.out.println(spelParser.parseExpression("Second: [[#faker.name().fullName()]]", temParserCtx).getValue(ctx));
		System.out.println(spelParser.parseExpression("[[T(System).currentTimeMillis()]]", temParserCtx).getValue(ctx, Long.class));
		System.out.println(spelParser.parseExpression("No SpEL", temParserCtx).getValue(ctx));

	}

	public void bla() throws Exception {
		String json = "{\n" +
				"  \"root\": [\n" +
				"   {\"k\":\"v1\"},\n" +
				"   {\"k\":\"v2\"}\n" +
				"  ]\n" +
				"}";
		ObjectMapper mapper = new ObjectMapper();
		Map root = (Map) mapper.readValue(json, Object.class);
		Expression expression = spelParser.parseExpression("root.?[k == 'v1']");
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new MapAccessor());
		System.out.println(expression.getValue(ctx, root));

	}

}
