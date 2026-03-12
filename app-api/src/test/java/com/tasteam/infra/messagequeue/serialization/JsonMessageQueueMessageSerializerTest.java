package com.tasteam.infra.messagequeue.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.MessageQueueMessage;

@UnitTest
@DisplayName("[유닛](Message) JsonMessageQueueMessageSerializer 테스트")
class JsonMessageQueueMessageSerializerTest {

	private final JsonMessageQueueMessageSerializer serializer = new JsonMessageQueueMessageSerializer(
		new ObjectMapper());

	@Test
	@DisplayName("메시지를 직렬화 후 역직렬화하면 핵심 필드가 유지된다")
	void roundTrip_preservesMessageFields() {
		// given
		MessageQueueMessage source = new MessageQueueMessage(
			"evt.notification.dispatch.v1",
			"member-1",
			"{\"type\":\"WELCOME\"}".getBytes(StandardCharsets.UTF_8),
			Map.of("traceId", "trace-1"),
			Instant.parse("2026-03-11T12:34:56Z"),
			"msg-123");

		// when
		String serialized = serializer.serialize(source);
		MessageQueueMessage restored = serializer.deserialize(serialized);

		// then
		assertThat(restored.topic()).isEqualTo(source.topic());
		assertThat(restored.key()).isEqualTo(source.key());
		assertThat(restored.payload()).containsExactly(source.payload());
		assertThat(restored.headers()).isEqualTo(source.headers());
		assertThat(restored.occurredAt()).isEqualTo(source.occurredAt());
		assertThat(restored.messageId()).isEqualTo(source.messageId());
	}

	@Test
	@DisplayName("빈 문자열 역직렬화는 예외를 던진다")
	void deserialize_blank_throwsException() {
		assertThatThrownBy(() -> serializer.deserialize(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("비어 있습니다");
	}

	@Test
	@DisplayName("잘못된 JSON 역직렬화는 예외를 던진다")
	void deserialize_invalidJson_throwsException() {
		assertThatThrownBy(() -> serializer.deserialize("{not-json"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("역직렬화에 실패");
	}
}
