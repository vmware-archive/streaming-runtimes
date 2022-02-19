package com.tanzu.streaming.runtime.avro.data.faker.util;

import org.springframework.expression.ParserContext;

public class SpELTemplateParserContext implements ParserContext {

	@Override
	public String getExpressionPrefix() {
		return "[[";
	}

	@Override
	public String getExpressionSuffix() {
		return "]]";
	}

	@Override
	public boolean isTemplate() {
		return true;
	}
}
