package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.exception.MessageQueuePublishException;
import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;

@UnitTest
@DisplayName("[유닛](Message) KafkaMessageQueueProducer 테스트")
class KafkaMessageQueueProducerTest {

	@Test
	@DisplayName("Kafka 전송 요청이 접수되면 ack를 기다리지 않고 반환한다")
	void publish_returnsWithoutWaitingForBrokerAck() throws Exception {
		@SuppressWarnings("unchecked") KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
		QueueMessageSerializer serializer = mock(QueueMessageSerializer.class);
		QueueMessage message = new QueueMessage(
			"evt.test",
			"k1",
			new ObjectMapper().readTree("{\"ok\":true}"),
			Map.of("eventType", "TEST"),
			Instant.now(),
			"msg-1");
		CompletableFuture<SendResult<String, String>> pendingFuture = new CompletableFuture<>();
		when(serializer.serialize(message)).thenReturn("{\"envelope\":true}");
		when(kafkaTemplate.send("evt.test", "k1", "{\"envelope\":true}")).thenReturn(pendingFuture);

		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		KafkaMessageQueueProducer producer = new KafkaMessageQueueProducer(kafkaTemplate, properties, serializer);

		assertThatCode(() -> producer.publish(message)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Kafka 전송 요청 자체가 실패하면 MessageQueuePublishException으로 래핑한다")
	void publish_wrapsImmediateSendFailure() throws Exception {
		@SuppressWarnings("unchecked") KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
		QueueMessageSerializer serializer = mock(QueueMessageSerializer.class);
		QueueMessage message = new QueueMessage(
			"evt.test",
			"k1",
			new ObjectMapper().readTree("{\"ok\":false}"),
			Map.of(),
			Instant.now(),
			"msg-2");
		when(serializer.serialize(message)).thenReturn("{\"envelope\":false}");
		when(kafkaTemplate.send("evt.test", "k1", "{\"envelope\":false}"))
			.thenThrow(new RuntimeException("broker down"));

		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		KafkaMessageQueueProducer producer = new KafkaMessageQueueProducer(kafkaTemplate, properties, serializer);

		assertThatThrownBy(() -> producer.publish(message))
			.isInstanceOf(MessageQueuePublishException.class)
			.hasMessageContaining("topic=evt.test")
			.hasMessageContaining("messageId=msg-2");
	}
}
