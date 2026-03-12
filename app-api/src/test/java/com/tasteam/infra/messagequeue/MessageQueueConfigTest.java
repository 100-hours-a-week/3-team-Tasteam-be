package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

@UnitTest
@DisplayName("[유닛](Message) MessageQueueConfig 단위 테스트")
class MessageQueueConfigTest {

	private final MessageQueueConfig config = new MessageQueueConfig();

	@Test
	@DisplayName("provider가 none이면 NoOp producer를 반환한다")
	void producer_noneProvider_returnsNoOpProducer() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);

		// when
		MessageQueueProducer producer = config.messageQueueProducer(properties, traceService, null, null);

		// then
		assertThat(producer).isInstanceOf(TracingMessageQueueProducer.class);
	}

	@Test
	@DisplayName("provider가 redis-stream이면 RedisStream producer를 반환한다")
	void producer_redisProvider_returnsRedisStreamProducer() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);
		StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

		// when
		MessageQueueProducer producer = config.messageQueueProducer(properties, traceService, stringRedisTemplate,
			null);

		// then
		assertThat(producer).isInstanceOf(TracingMessageQueueProducer.class);
	}

	@Test
	@DisplayName("provider가 redis-stream인데 redis 템플릿이 없으면 producer 생성에 실패한다")
	void producer_redisProvider_withoutRedisTemplate_throwsException() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider("redis-stream");
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);

		// when & then
		assertThatThrownBy(() -> config.messageQueueProducer(properties, traceService, null, null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("StringRedisTemplate");
	}

	@Test
	@DisplayName("provider가 none이면 NoOp consumer를 반환한다")
	void consumer_noneProvider_returnsNoOpConsumer() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);
		ObjectMapper objectMapper = new ObjectMapper();

		// when
		MessageQueueConsumer consumer = config.messageQueueConsumer(properties, traceService, objectMapper, null, null,
			null);

		// then
		assertThat(consumer).isInstanceOf(TracingMessageQueueConsumer.class);
	}

	@Test
	@DisplayName("provider가 redis-stream이면 RedisStream consumer를 반환한다")
	void consumer_redisProvider_returnsRedisStreamConsumer() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked") StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer = mock(
			StreamMessageListenerContainer.class);

		// when
		MessageQueueConsumer consumer = config.messageQueueConsumer(properties, traceService, objectMapper,
			stringRedisTemplate, listenerContainer, null);

		// then
		assertThat(consumer).isInstanceOf(TracingMessageQueueConsumer.class);
	}

	@Test
	@DisplayName("provider가 redis-stream인데 리스너 컨테이너가 없으면 consumer 생성에 실패한다")
	void consumer_redisProvider_withoutListenerContainer_throwsException() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider("redis-stream");
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

		// when & then
		assertThatThrownBy(
			() -> config.messageQueueConsumer(properties, traceService, objectMapper, stringRedisTemplate, null, null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("StreamMessageListenerContainer");
	}
}
