package com.tanzu.streaming.runtime.data.generator;

public class MapTest {

	public static void main(String[] args) {
		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/map1.avsc"), 3));
	}

}
