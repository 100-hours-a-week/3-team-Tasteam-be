package com.tasteam.infra.messagequeue.serialization;

import com.tasteam.infra.messagequeue.MessageQueueMessage;

public interface MessageQueueMessageSerializer {

	String serialize(MessageQueueMessage message);

	MessageQueueMessage deserialize(String serialized);
}
