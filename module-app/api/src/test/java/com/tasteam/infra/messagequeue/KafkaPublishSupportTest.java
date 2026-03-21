package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.exception.MessageQueuePublishException;
import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;

@UnitTest
@DisplayName("[유닛](Message) KafkaPublishSupport 테스트")
class KafkaPublishSupportTest {

	@Test
	@DisplayName("Kafka 전송이 성공하면 예외 없이 반환한다")
	void publish_success_returnsNormally() throws Exception {
		// given
		@SuppressWarnings("unchecked") KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
		QueueMessageSerializer serializer = mock(QueueMessageSerializer.class);
		QueueMessage message = new QueueMessage(
			"evt.test",
			"k1",
			new ObjectMapper().readTree("{\"ok\":true}"),
			Map.of("eventType", "TEST"),
			Instant.now(),
			"msg-1");
		@SuppressWarnings("unchecked") CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture
			.completedFuture(
				mock(SendResult.class));
		when(serializer.createMessage("evt.test", "k1", Map.of("ok", true), Map.of("eventType", "TEST")))
			.thenReturn(message);
		when(serializer.serialize(message)).thenReturn("{\"envelope\":true}");
		when(kafkaTemplate.send("evt.test", "k1", "{\"envelope\":true}")).thenReturn(successFuture);

		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		properties.getProducer().setSendTimeoutMillis(5000L);
		KafkaPublishSupport publishSupport = new KafkaPublishSupport(kafkaTemplate, properties, serializer);

		// when & then
		assertThatCode(() -> publishSupport.publish("evt.test", "k1", Map.of("ok", true), Map.of("eventType", "TEST")))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Kafka 전송이 실패하면 MessageQueuePublishException으로 래핑한다")
	void publish_failure_wrapsException() throws Exception {
		// given
		@SuppressWarnings("unchecked") KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
		QueueMessageSerializer serializer = mock(QueueMessageSerializer.class);
		QueueMessage message = new QueueMessage(
			"evt.test",
			"k1",
			new ObjectMapper().readTree("{\"ok\":false}"),
			Map.of(),
			Instant.now(),
			"msg-2");
		CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new ExecutionException(new RuntimeException("broker down")));
		when(serializer.createMessage("evt.test", "k1", Map.of("ok", false), Map.of())).thenReturn(message);
		when(serializer.serialize(message)).thenReturn("{\"envelope\":false}");
		when(kafkaTemplate.send("evt.test", "k1", "{\"envelope\":false}")).thenReturn(failedFuture);

		KafkaMessageQueueProperties properties = new KafkaMessageQueueProperties();
		properties.getProducer().setSendTimeoutMillis(5000L);
		KafkaPublishSupport publishSupport = new KafkaPublishSupport(kafkaTemplate, properties, serializer);

		// when & then
		assertThatThrownBy(() -> publishSupport.publish("evt.test", "k1", Map.of("ok", false), Map.of()))
			.isInstanceOf(MessageQueuePublishException.class)
			.hasMessageContaining("topic=evt.test")
			.hasMessageContaining("messageId=msg-2");
	}
}
