package com.tanzu.streaming.runtime.data.generator;

import java.util.Iterator;

import org.apache.avro.generic.GenericData;

public class QuickStart2 {
	public static void main(String[] args) {

		Iterator<GenericData.Record> iterator = new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/user3.yaml"),
				3)
				.iterator();

		while (iterator.hasNext()) {
			System.out.println(iterator.next());
		}

	}
}
