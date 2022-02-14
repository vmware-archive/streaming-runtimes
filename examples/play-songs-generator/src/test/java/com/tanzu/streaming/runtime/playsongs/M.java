package com.tanzu.streaming.runtime.playsongs;

import java.io.IOException;
import java.io.InputStream;

import com.github.javafaker.Faker;
import org.apache.avro.Schema;

import org.springframework.core.io.DefaultResourceLoader;

public class M {
	public static void main(String[] args) throws IOException {
		InputStream is = new DefaultResourceLoader().getResource("classpath:/avro/user.avsc").getInputStream();
		Schema schema = new Schema.Parser().parse(is);
		new MyRandomData(schema, 100).forEach(System.out::println);


		Faker faker = new Faker();
		System.out.println(faker.address().fullAddress());
		System.out.println(faker.music().genre());

		System.out.println(faker.hitchhikersGuideToTheGalaxy().specie());
		System.out.println(faker.expression("#{number.number_between '1','10'}"));
		System.out.println(faker.expression("#{name.full_name}"));
	}
}
