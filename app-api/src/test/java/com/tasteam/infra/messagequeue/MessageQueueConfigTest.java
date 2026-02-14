package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessageQueueConfigTest {

	private final MessageQueueConfig config = new MessageQueueConfig();

	@Test
	void producer_noneProvider_returnsNoOpProducer() {
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");

		MessageQueueProducer producer = config.messageQueueProducer(properties);

		assertThat(producer).isInstanceOf(NoOpMessageQueueProducer.class);
	}

	@Test
	void producer_redisProvider_returnsUnsupportedProducer() {
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");

		MessageQueueProducer producer = config.messageQueueProducer(properties);

		assertThat(producer).isInstanceOf(UnsupportedMessageQueueProducer.class);
	}

	@Test
	void consumer_noneProvider_returnsNoOpConsumer() {
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");

		MessageQueueConsumer consumer = config.messageQueueConsumer(properties);

		assertThat(consumer).isInstanceOf(NoOpMessageQueueConsumer.class);
	}
}
