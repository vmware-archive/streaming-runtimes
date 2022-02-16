package com.tanzu.streaming.runtime.avro.data.faker;

import org.springframework.expression.ParserContext;

public class SpELTemplateParserContext implements ParserContext {

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
