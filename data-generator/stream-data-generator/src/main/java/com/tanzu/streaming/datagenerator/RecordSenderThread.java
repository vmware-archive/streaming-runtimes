package com.tanzu.streaming.datagenerator;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.tanzu.streaming.runtime.avro.data.faker.AvroRandomDataFaker;
import com.tanzu.streaming.runtime.avro.data.faker.DataGenerator;
import org.apache.avro.generic.GenericData;

public class RecordSenderThread implements Runnable {

	private final MessageSender messageSender;
	private final AvroRandomDataFaker dataFaker;
	private final StreamDataGeneratorApplicationProperties.Topic topicProperties;
	private final AtomicBoolean exitFlag;

	public RecordSenderThread(
			MessageSender messageSender,
			AvroRandomDataFaker dataFaker,
			StreamDataGeneratorApplicationProperties.Topic topicProperties,
			AtomicBoolean exitFlag) {

		this.messageSender = messageSender;
		this.dataFaker = dataFaker;
		this.topicProperties = topicProperties;
		this.exitFlag = exitFlag;
	}

	@Override
	public void run() {

		//logger.info(String.format("New data generation thread for: %s", this.topicProperties.getTopicName()));

		final AtomicLong messageKey = new AtomicLong(System.currentTimeMillis());
		List<GenericData.Record> records = DataGenerator.generateRecords(dataFaker);
		List<GenericData.Record> nonNullRecords = records.stream().filter(Objects::nonNull)
				.collect(Collectors.toList());
		Iterator<GenericData.Record> iterator = nonNullRecords.iterator();

		if (!this.topicProperties.isSkipSending()) {
			while (!this.exitFlag.get() && iterator.hasNext()) {
				GenericData.Record record = iterator.next();
				Object messageValue = toValueFormat(record);
				this.messageSender.send(messageKey.incrementAndGet(), messageValue);
				try {
					Thread.sleep(this.topicProperties.getBatch().getMessageDelay().toMillis());
				}
				catch (InterruptedException e) {
					//e.printStackTrace();
				}
			}
		}
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
