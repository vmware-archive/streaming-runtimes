package com.tanzu.streaming.runtime.avro.data.faker;

public class MapTest {

	public static void main(String[] args) {
		DataGenerator.generateRecords(
						DataGenerator.uriToSchema("classpath:/avro/map1.avsc"), 3)
				.forEach(System.out::println);
	}

}
