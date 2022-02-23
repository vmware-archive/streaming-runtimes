package com.tanzu.streaming.runtime.data.generator;

public class MapTest {

	public static void main(String[] args) {
		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/map0.yaml"), 3));

		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/map1.yaml"), 3));
	}

}
