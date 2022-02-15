package com.tanzu.streaming.runtime.avro.data.faker;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class Main {


	public static void main(String[] args) throws Exception {
		new Main().bla2();
	}

	public void bla2() {
		Faker faker = new Faker();
		System.out.println(faker.expression("[[test]]"));


		Map<String, String> myMap = new HashMap<>();
		myMap.put("foo", "bar");



		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("['foo']");
		//Expression exp = parser.parseExpression("'Hello [[myMap['foo']]]'", new TemplateParserContext());
		//Expression exp = parser.parseExpression("'Hello [[new net.datafaker.Faker().date().birthday()]]'", new TemplateParserContext());

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new MapAccessor());

		System.out.println(exp.getValue(context, myMap));

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
		Expression expression = new SpelExpressionParser().parseExpression("root.?[k == 'v1']");
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new MapAccessor());
		System.out.println(expression.getValue(ctx, root));
	}

	public static class TemplateParserContext implements ParserContext {

		public String getExpressionPrefix() {
			return "[[";
		}

		public String getExpressionSuffix() {
			return "]]";
		}

		public boolean isTemplate() {
			return true;
		}
	}
}
