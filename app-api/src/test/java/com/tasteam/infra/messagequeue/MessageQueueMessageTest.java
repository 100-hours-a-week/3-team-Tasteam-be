package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("메시지큐 메시지")
class MessageQueueMessageTest {

	@Test
	@DisplayName("팩토리 메서드로 메시지를 만들면 기본 헤더와 식별 정보가 채워진다")
	void of_createsMessageWithDefaults() {
		// when
		MessageQueueMessage message = MessageQueueMessage.of("notification.created", "member-1", new byte[] {1, 2, 3});

		// then
		assertThat(message.topic()).isEqualTo("notification.created");
		assertThat(message.key()).isEqualTo("member-1");
		assertThat(message.payload()).containsExactly(1, 2, 3);
		assertThat(message.headers()).isEmpty();
		assertThat(message.messageId()).isNotBlank();
		assertThat(message.occurredAt()).isNotNull();
	}

	@Test
	@DisplayName("생성 후 원본 payload와 headers를 변경해도 메시지 내부 값은 유지된다")
	void constructor_defensivelyCopiesPayloadAndHeaders() {
		// given
		byte[] payload = new byte[] {9, 8, 7};
		Map<String, String> headers = new HashMap<>();
		headers.put("source", "test");

		// when
		MessageQueueMessage message = new MessageQueueMessage(
			"group.member.joined",
			"group-1",
			payload,
			headers,
			null,
			null);

		payload[0] = 1;
		headers.put("source", "changed");

		// then
		assertThat(message.payload()).containsExactly(9, 8, 7);
		assertThat(message.headers().get("source")).isEqualTo("test");
	}

	@Test
	@DisplayName("topic이 비어 있으면 메시지를 생성할 수 없다")
	void constructor_blankTopic_throwsException() {
		// when & then
		assertThatThrownBy(() -> new MessageQueueMessage(" ", "k", new byte[] {1}, Map.of(), null, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("topic은 필수");
	}
}
