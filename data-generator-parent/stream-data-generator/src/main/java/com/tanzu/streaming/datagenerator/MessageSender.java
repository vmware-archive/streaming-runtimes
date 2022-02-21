package com.tanzu.streaming.datagenerator;

public interface MessageSender {
	void send(Object key, Object value);
}
