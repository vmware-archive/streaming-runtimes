package com.tanzu.streaming.datagenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.StringUtils;

public class KafkaMessageSender implements MessageSender {

	private final KafkaTemplate<Long, Object> kafkaTemplate;

	public KafkaMessageSender(String kafkaServer,
			String schemaRegistryServer,
			StreamDataGeneratorApplicationProperties.ValueFormat serType,
			String topicName,
			Class<? extends Serializer> keySerializerClass) {

		this.kafkaTemplate = createKafkaTemplate(
				kafkaServer, schemaRegistryServer, serType, topicName, keySerializerClass);
	}

	@Override
	public void send(Object key, Object value) {
		this.kafkaTemplate.sendDefault((Long)key, value);
	}

	public KafkaTemplate<Long, Object> createKafkaTemplate(
			String kafkaServer,
			String schemaRegistryServer,
			StreamDataGeneratorApplicationProperties.ValueFormat serType,
			String topicName,
			Class<? extends Serializer> keySerializerClass) {

		Class<? extends Serializer> valueSerializerClass = null;
		if (serType == StreamDataGeneratorApplicationProperties.ValueFormat.JSON) {
			valueSerializerClass = JsonSerializer.class;
		}
		else if (serType == StreamDataGeneratorApplicationProperties.ValueFormat.AVRO) {
			valueSerializerClass = KafkaAvroSerializer.class;
		}
		else if (serType == StreamDataGeneratorApplicationProperties.ValueFormat.AVRO_SCHEMA_REGISTRY) {
			GenericAvroSerializer avroSerializer = new GenericAvroSerializer();
			avroSerializer.configure(Collections.singletonMap(
							AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
							schemaRegistryServer),
					false);
			valueSerializerClass = GenericAvroSerializer.class;
		}

		Map<String, Object> kafkaProperties = new HashMap<>(getCommonKafkaProperties(kafkaServer, schemaRegistryServer));
		kafkaProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
		kafkaProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);

		DefaultKafkaProducerFactory<Long, Object> pf = new DefaultKafkaProducerFactory<>(kafkaProperties);
		KafkaTemplate<Long, Object> kafkaTemplate = new KafkaTemplate<>(pf, true);

		kafkaTemplate.setDefaultTopic(topicName);

		return kafkaTemplate;
	}

	public Map<String, Object> getCommonKafkaProperties(String kafkaServer, String schemaRegistryServer) {
		Map<String, Object> commonKafkaProperties = new HashMap<>();
		commonKafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
		if (StringUtils.hasText(schemaRegistryServer)) {
			commonKafkaProperties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryServer);
		}
		commonKafkaProperties.put(ProducerConfig.RETRIES_CONFIG, 0);
		commonKafkaProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
		commonKafkaProperties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
		commonKafkaProperties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
		return commonKafkaProperties;
	}
}
