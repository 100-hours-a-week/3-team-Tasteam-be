package com.tasteam.infra.messagequeue;

public interface TopicNamingPolicy {

	TopicSet resolve(QueueTopic topic);

	default String main(QueueTopic topic) {
		return resolve(topic).main();
	}

	default String dlq(QueueTopic topic) {
		return resolve(topic).dlq();
	}

	String dlq(String sourceTopic);
}
