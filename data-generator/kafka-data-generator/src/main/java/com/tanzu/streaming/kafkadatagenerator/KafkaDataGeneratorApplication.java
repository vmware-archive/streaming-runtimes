/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tanzu.streaming.kafkadatagenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.tanzu.streaming.runtime.avro.data.faker.AvroRandomDataFaker;
import com.tanzu.streaming.runtime.avro.data.faker.DataGenerator;
import com.tanzu.streaming.runtime.avro.data.faker.util.SharedFieldValuesContext;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.StringUtils;

@SpringBootApplication
@EnableConfigurationProperties(KafkaDataGeneratorApplicationProperties.class)
public class KafkaDataGeneratorApplication implements CommandLineRunner {

	protected static final Logger logger = LoggerFactory.getLogger(KafkaDataGeneratorApplication.class);

	private final KafkaDataGeneratorApplicationProperties properties;

	public KafkaDataGeneratorApplication(@Autowired KafkaDataGeneratorApplicationProperties appProperties) {
		this.properties = appProperties;
	}

	public static void main(String[] args) {
		SpringApplication.run(KafkaDataGeneratorApplication.class, args);
	}

	@Override
	public void run(String... args) {

		SharedFieldValuesContext fieldCorrelationContext = new SharedFieldValuesContext(new Random());

		ScheduledExecutorService scheduler =
				Executors.newScheduledThreadPool(this.properties.getScheduledThreadPoolSize());

		AtomicBoolean exitFlag = new AtomicBoolean(false);

		List<ScheduledFuture> scheduledFutures = new ArrayList<>();

		for (KafkaDataGeneratorApplicationProperties.Topic topicProperties : this.properties.getTopics()) {

			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("avro-schema", topicProperties.getAvroSchema());
				entries.put("avro-schema-uri", topicProperties.getAvroSchemaUri());
			});

			// TODO parametrize the key serializer.
			KafkaTemplate<Long, Object> kafkaTemplate =
					kafkaTemplate(topicProperties.getValueFormat(), topicProperties.getTopicName(), LongSerializer.class);

			Schema avroSchema = StringUtils.hasText(topicProperties.getAvroSchema()) ?
					DataGenerator.toSchema(topicProperties.getAvroSchema()) :
					DataGenerator.resourceToSchema(topicProperties.getAvroSchemaUri());

			AvroRandomDataFaker dataFaker = DataGenerator.dataFaker(
					avroSchema,
					topicProperties.getBatch().getSize(),
					fieldCorrelationContext,
					System.currentTimeMillis());

			ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
					new TopicRecordSender(kafkaTemplate, dataFaker, topicProperties, exitFlag),
					topicProperties.getBatch().getInitialDelay().toMillis(),
					topicProperties.getBatch().getDelay().toMillis(),
					TimeUnit.MILLISECONDS);

			scheduledFutures.add(future);
		}

		try {
			Thread.sleep(this.properties.getTerminateAfter().toMillis());
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

		exitFlag.set(true);
		scheduledFutures.forEach(future -> future.cancel(true));
		awaitTerminationAfterShutdown(scheduler);
	}

	private void awaitTerminationAfterShutdown(ExecutorService threadPool) {
		threadPool.shutdown();
		try {
			if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
				threadPool.shutdownNow();
			}
		}
		catch (InterruptedException ex) {
			threadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	public static class TopicRecordSender implements Runnable {

		private final KafkaTemplate<Long, Object> kafkaTemplate;
		private final AvroRandomDataFaker dataFaker;
		private final KafkaDataGeneratorApplicationProperties.Topic topicProperties;
		private final AtomicBoolean exitFlag;

		public TopicRecordSender(
				KafkaTemplate<Long, Object> kafkaTemplate,
				AvroRandomDataFaker dataFaker,
				KafkaDataGeneratorApplicationProperties.Topic topicProperties,
				AtomicBoolean exitFlag) {

			this.kafkaTemplate = kafkaTemplate;
			this.dataFaker = dataFaker;
			this.topicProperties = topicProperties;
			this.exitFlag = exitFlag;
		}

		@Override
		public void run() {
			final AtomicLong messageKey = new AtomicLong(System.currentTimeMillis());
			List<GenericData.Record> records = DataGenerator.generateRecords(dataFaker);
			Iterator<GenericData.Record> iterator = records.iterator();

			if (!this.topicProperties.isSkipSending()) {
				while (!this.exitFlag.get() && iterator.hasNext()) {
					GenericData.Record record = iterator.next();
					Object messageValue = toValueFormat(record);
					this.kafkaTemplate.sendDefault(messageKey.incrementAndGet(), messageValue);
					try {
						logger.info(String.format("Send to %s : %s", this.topicProperties.getTopicName(), messageValue));
						Thread.sleep(this.topicProperties.getBatch().getMessageDelay().toMillis());
					}
					catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}
			}
//			logger.info(String.format("Finish thread: %s : %s", this.topicProperties.getTopicName(), topicProperties.getKeyFieldName()));
		}

		private Object toValueFormat(GenericData.Record record) {
			switch (this.topicProperties.getValueFormat()) {
			case JSON:
				return DataGenerator.toJsonObjectNode(record);
			case YAML:
				return DataGenerator.toYaml(record);
			default:
				return record;
			}
		}
	}

	private KafkaTemplate<Long, Object> kafkaTemplate(KafkaDataGeneratorApplicationProperties.ValueFormat serType,
			String topicName,
			Class<? extends Serializer> keySerializerClass) {

		Class<? extends Serializer> valueSerializerClass = null;
		if (serType == KafkaDataGeneratorApplicationProperties.ValueFormat.JSON) {
			valueSerializerClass = JsonSerializer.class;
		}
		else if (serType == KafkaDataGeneratorApplicationProperties.ValueFormat.AVRO) {
			valueSerializerClass = KafkaAvroSerializer.class;
		}
		else {
			GenericAvroSerializer avroSerializer = new GenericAvroSerializer();
			avroSerializer.configure(Collections.singletonMap(
							AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
							this.properties.getSchemaRegistryServer()),
					false);
			valueSerializerClass = GenericAvroSerializer.class;
		}

		Map<String, Object> kafkaProperties = new HashMap<>(this.properties.getCommonKafkaProperties());
		kafkaProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
		kafkaProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);

		DefaultKafkaProducerFactory<Long, Object> pf = new DefaultKafkaProducerFactory<>(kafkaProperties);
		KafkaTemplate<Long, Object> kafkaTemplate = new KafkaTemplate<>(pf, true);

		kafkaTemplate.setDefaultTopic(topicName);

		return kafkaTemplate;
	}

}
