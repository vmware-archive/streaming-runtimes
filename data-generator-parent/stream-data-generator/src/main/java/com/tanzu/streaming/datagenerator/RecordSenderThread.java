package com.tanzu.streaming.datagenerator;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.tanzu.streaming.runtime.data.generator.DataGenerator;
import com.tanzu.streaming.runtime.data.generator.DataUtil;
import org.apache.avro.generic.GenericData;

public class RecordSenderThread implements Runnable {

	private final MessageSender messageSender;
	private final DataGenerator avroRandomData;
	private final StreamDataGeneratorApplicationProperties.RecordStream topicProperties;
	private final AtomicBoolean exitFlag;

	public RecordSenderThread(
			MessageSender messageSender,
			DataGenerator dataFaker,
			StreamDataGeneratorApplicationProperties.RecordStream topicProperties,
			AtomicBoolean exitFlag) {

		this.messageSender = messageSender;
		this.avroRandomData = dataFaker;
		this.topicProperties = topicProperties;
		this.exitFlag = exitFlag;
	}

	@Override
	public void run() {

		final AtomicLong messageKey = new AtomicLong(System.currentTimeMillis());

		Iterator<GenericData.Record> iterator = avroRandomData.iterator();
		if (!this.topicProperties.isSkipSending()) {
			while (!this.exitFlag.get() && iterator.hasNext()) {
				GenericData.Record record = iterator.next();
				if (record != null) {
					Object messageValue = toValueFormat(record);
					this.messageSender.send(messageKey.incrementAndGet(), messageValue);
				}
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
			return DataUtil.toJsonObjectNode(record);
		case YAML:
			return DataUtil.toYaml(record);
		default:
			return record;
		}
	}
}
