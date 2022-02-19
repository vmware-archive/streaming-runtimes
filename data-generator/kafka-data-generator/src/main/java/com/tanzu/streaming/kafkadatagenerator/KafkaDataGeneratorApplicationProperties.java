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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kafka.data.generator")
public class KafkaDataGeneratorApplicationProperties {

	public enum ValueFormat {AVRO, AVRO_SCHEMA_REGISTRY, JSON, YAML}

	private int scheduledThreadPoolSize = 10;

	/**
	 * Kafka Broker URL
	 */
	@NotBlank
	private String kafkaServer = "localhost:9094";

	/**
	 * Optional Kafka Confluence schema registry. Required if the AVRO_SCHEMA_REGISTRY format is used.
	 */
	private String schemaRegistryServer = "http://localhost:8081";

	/**
	 * Terminate the generator after given duration of time. Defaults to 1 billion years, e.g. run forever.
	 */
	private Duration terminateAfter = Duration.ofDays(1000000 * 356); // 1B years

	/**
	 * Configuration for each topic.
	 */
	@Valid
	private List<Topic> topics = new ArrayList<>();

	public int getScheduledThreadPoolSize() {
		return scheduledThreadPoolSize;
	}

	public void setScheduledThreadPoolSize(int scheduledThreadPoolSize) {
		this.scheduledThreadPoolSize = scheduledThreadPoolSize;
	}

	public String getKafkaServer() {
		return kafkaServer;
	}

	public void setKafkaServer(String kafkaServer) {
		this.kafkaServer = kafkaServer;
	}

	public String getSchemaRegistryServer() {
		return schemaRegistryServer;
	}

	public void setSchemaRegistryServer(String schemaRegistryServer) {
		this.schemaRegistryServer = schemaRegistryServer;
	}

	public Duration getTerminateAfter() {
		return terminateAfter;
	}

	public void setTerminateAfter(Duration terminateAfter) {
		this.terminateAfter = terminateAfter;
	}

	public List<Topic> getTopics() {
		return topics;
	}

	/**
	 * TODO to let users provide their own, custom, Kafka properties!
	 *
	 * Common Kafka connection properties.
	 *
	 * @return Returns an opinionated Kafka connection configuration based on the existing properties.
	 */
	public Map<String, Object> getCommonKafkaProperties() {
		Map<String, Object> commonKafkaProperties = new HashMap<>();
		commonKafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.getKafkaServer());
		if (StringUtils.hasText(this.getSchemaRegistryServer())) {
			commonKafkaProperties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.getSchemaRegistryServer());
		}
		commonKafkaProperties.put(ProducerConfig.RETRIES_CONFIG, 0);
		commonKafkaProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
		commonKafkaProperties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
		commonKafkaProperties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
		return commonKafkaProperties;
	}

	public static class Topic {
		/**
		 * Kafka topic name.
		 */
		@NotBlank
		private String topicName;

		/**
		 * Annotated Avro schema used by the DataFaker for data generation.
		 * Only one of avroSchema or avroSchemaUri is allowed.
		 */
		private Resource avroSchemaUri;

		/**
		 * Annotated Avro Schema text content.
		 * Only one of avroSchema or avroSchemaUri is allowed.
		 */
		private String avroSchema;

		/**
		 * Value serialization format sent to Kafka topic. Defaults to JSON.
		 */
		@NotNull
		private ValueFormat valueFormat = ValueFormat.JSON;

		/**
		 * The data generator produces new data in batch of records.
		 */
		@Valid
		private Batch batch = new Batch();

		/**
		 * If set to true the random data is generated but not send to the topic.
		 */
		private boolean skipSending = false;

		public String getTopicName() {
			return topicName;
		}

		public void setTopicName(String topicName) {
			this.topicName = topicName;
		}

		public Resource getAvroSchemaUri() {
			return avroSchemaUri;
		}

		public void setAvroSchemaUri(Resource avroSchemaUri) {
			this.avroSchemaUri = avroSchemaUri;
		}

		public String getAvroSchema() {
			return avroSchema;
		}

		public void setAvroSchema(String avroSchema) {
			this.avroSchema = avroSchema;
		}

		public ValueFormat getValueFormat() {
			return valueFormat;
		}

		public void setValueFormat(ValueFormat valueFormat) {
			this.valueFormat = valueFormat;
		}

		public Batch getBatch() {
			return batch;
		}

		public boolean isSkipSending() {
			return skipSending;
		}

		public void setSkipSending(boolean skipSending) {
			this.skipSending = skipSending;
		}

		@Override
		public String toString() {
			return "Topic{" +
					"topicName='" + topicName + '\'' +
					", avroSchemaUri=" + avroSchemaUri +
					", avroSchema='" + avroSchema + '\'' +
					", valueFormat=" + valueFormat +
					", batch=" + batch +
					'}';
		}
	}

	public static class Batch {
		/**
		 * Number of the records produced in one batch generation iteration.
		 */
		@Positive
		private int size;

		/**
		 * The time to delay before the first batch records generation.
		 */
		@NotNull
		private Duration initialDelay = Duration.ofMillis(10);

		/**
		 * The delay between the termination of one records batch generation and the commencement of the next.
		 * Defaults to 1 billion years, e.g. no consecutive batches are generated.
		 */
		@NotNull
		private Duration delay = Duration.ofDays(1000000 * 356); // 1B years

		/**
		 * The delay between sending each message in the batch to the Kafka topic.
		 */
		@NotNull
		private Duration messageDelay = Duration.ofSeconds(1);

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public Duration getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(Duration initialDelay) {
			this.initialDelay = initialDelay;
		}

		public Duration getDelay() {
			return delay;
		}

		public void setDelay(Duration delay) {
			this.delay = delay;
		}

		public Duration getMessageDelay() {
			return messageDelay;
		}

		public void setMessageDelay(Duration messageDelay) {
			this.messageDelay = messageDelay;
		}

		@Override
		public String toString() {
			return "Batch{" +
					"size=" + size +
					", initialDelay=" + initialDelay +
					", delay=" + delay +
					", messageDelay=" + messageDelay +
					'}';
		}
	}
}
