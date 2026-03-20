package com.tasteam.infra.messagequeue.serialization;

import java.util.Map;

import com.tasteam.infra.messagequeue.QueueMessage;

public interface QueueMessageSerializer {

	QueueMessage createMessage(String topic, String key, Object payload, Map<String, String> headers);

	String serialize(QueueMessage message);

	QueueMessage deserialize(String serialized);
}
