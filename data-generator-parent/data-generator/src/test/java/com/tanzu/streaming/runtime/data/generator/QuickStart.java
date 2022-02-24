package com.tanzu.streaming.runtime.data.generator;

public class QuickStart {
	public static void main(String[] args) {
DataUtil.print(
	new DataGenerator(DataUtil.contentToSchema(""
		+ "namespace: io.simple.clicksteram\n"
		+ "type: record\n"
		+ "name: User\n"
		+ "fields:\n"
		+ "  - name: id\n"
		+ "    type: string\n"
		+ "    doc: \"#{id_number.valid}\" # Using Java Faker.\n"
		+ "  - name: sendAt\n"
		+ "    type:\n"
		+ "      type: long\n"
		+ "      logicalType: timestamp-millis\n"
		+ "    doc: \"[[T(System).currentTimeMillis()]]\" # using Spring SpEL\n"
		+ "  - name: fullName\n"
		+ "    type: string\n"
		+ "    doc: \"#{name.fullName}\"\n"
		+ "  - name: email\n"
		+ "    type: string\n"
		+ "    doc: \"#{internet.emailAddress}\"\n"
		+ "  - name: age\n"
		+ "    type: int\n"
		+ "    doc: \"#{number.number_between '8','80'}\""),
	3) // instance count
);
	}
}
