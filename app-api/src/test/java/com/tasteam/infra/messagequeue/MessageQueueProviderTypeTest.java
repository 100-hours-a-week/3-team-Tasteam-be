package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MessageQueueProviderTypeTest {

	@Test
	void from_mapsAliasesToProviderType() {
		assertThat(MessageQueueProviderType.from("none")).isEqualTo(MessageQueueProviderType.NONE);
		assertThat(MessageQueueProviderType.from("redis-stream")).isEqualTo(MessageQueueProviderType.REDIS_STREAM);
		assertThat(MessageQueueProviderType.from("redis_stream")).isEqualTo(MessageQueueProviderType.REDIS_STREAM);
		assertThat(MessageQueueProviderType.from("kafka")).isEqualTo(MessageQueueProviderType.KAFKA);
	}

	@Test
	void from_unknownValue_throwsException() {
		assertThatThrownBy(() -> MessageQueueProviderType.from("rabbitmq"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("지원하지 않는 message queue provider");
	}
}
