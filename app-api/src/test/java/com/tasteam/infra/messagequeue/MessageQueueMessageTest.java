package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MessageQueueMessageTest {

	@Test
	void of_createsMessageWithDefaults() {
		MessageQueueMessage message = MessageQueueMessage.of("notification.created", "member-1", new byte[] {1, 2, 3});

		assertThat(message.topic()).isEqualTo("notification.created");
		assertThat(message.key()).isEqualTo("member-1");
		assertThat(message.payload()).containsExactly(1, 2, 3);
		assertThat(message.headers()).isEmpty();
		assertThat(message.messageId()).isNotBlank();
		assertThat(message.occurredAt()).isNotNull();
	}

	@Test
	void constructor_defensivelyCopiesPayloadAndHeaders() {
		byte[] payload = new byte[] {9, 8, 7};
		Map<String, String> headers = new HashMap<>();
		headers.put("source", "test");

		MessageQueueMessage message = new MessageQueueMessage(
			"group.member.joined",
			"group-1",
			payload,
			headers,
			null,
			null);

		payload[0] = 1;
		headers.put("source", "changed");

		assertThat(message.payload()).containsExactly(9, 8, 7);
		assertThat(message.headers().get("source")).isEqualTo("test");
	}

	@Test
	void constructor_blankTopic_throwsException() {
		assertThatThrownBy(() -> new MessageQueueMessage(" ", "k", new byte[] {1}, Map.of(), null, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("topic은 필수");
	}
}
