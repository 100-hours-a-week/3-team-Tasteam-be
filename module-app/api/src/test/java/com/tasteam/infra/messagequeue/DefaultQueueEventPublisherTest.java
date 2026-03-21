package com.tasteam.infra.messagequeue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.serialization.JsonQueueMessageSerializer;

@UnitTest
@DisplayName("[유닛](Message) DefaultQueueEventPublisher 테스트")
class DefaultQueueEventPublisherTest {

	@Test
	@DisplayName("QueueTopic/payload를 받아 브로커 메시지로 발행한다")
	void publish_sendsMessageToBroker() {
		MessageBrokerSender brokerSender = mock(MessageBrokerSender.class);
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		DefaultQueueEventPublisher publisher = new DefaultQueueEventPublisher(
			brokerSender,
			topicNamingPolicy,
			new JsonQueueMessageSerializer(new ObjectMapper()));

		publisher.publish(
			QueueTopic.NOTIFICATION_REQUESTED,
			"10",
			new SamplePayload("evt-1"),
			QueueEventHeaders.builder().eventType("SamplePayload").schemaVersion("v1").build());

		verify(brokerSender).send(any(QueueMessage.class));
	}

	private record SamplePayload(String eventId) {
	}
}
