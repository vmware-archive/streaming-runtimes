package com.tanzu.streaming.datagenerator.protocol;

import com.tanzu.streaming.datagenerator.MessageSender;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitMessageSender implements MessageSender {

	private final RabbitTemplate rabbitTemplate;

	public RabbitMessageSender() {
		this.rabbitTemplate = new RabbitTemplate();

	}

	@Override
	public void send(Object key, Object value) {
		//this.rabbitTemplate.send(value);
	}
}
