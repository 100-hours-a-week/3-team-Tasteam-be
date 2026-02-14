package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("메시지큐 설정 기반 빈 선택")
class MessageQueueConfigTest {

	private final MessageQueueConfig config = new MessageQueueConfig();

	@Test
	@DisplayName("provider가 none이면 NoOp producer를 반환한다")
	void producer_noneProvider_returnsNoOpProducer() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");

		// when
		MessageQueueProducer producer = config.messageQueueProducer(properties);

		// then
		assertThat(producer).isInstanceOf(NoOpMessageQueueProducer.class);
	}

	@Test
	@DisplayName("provider가 redis-stream이면 미구현 producer를 반환한다")
	void producer_redisProvider_returnsUnsupportedProducer() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");

		// when
		MessageQueueProducer producer = config.messageQueueProducer(properties);

		// then
		assertThat(producer).isInstanceOf(UnsupportedMessageQueueProducer.class);
	}

	@Test
	@DisplayName("provider가 none이면 NoOp consumer를 반환한다")
	void consumer_noneProvider_returnsNoOpConsumer() {
		// given
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");

		// when
		MessageQueueConsumer consumer = config.messageQueueConsumer(properties);

		// then
		assertThat(consumer).isInstanceOf(NoOpMessageQueueConsumer.class);
	}
}
