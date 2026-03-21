package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Message) QueueMessage 단위 테스트")
class QueueMessageTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	@DisplayName("팩토리 메서드로 메시지를 만들면 기본 헤더와 식별 정보가 채워진다")
	void of_createsMessageWithDefaults() throws Exception {
		// given
		JsonNode payload = MAPPER.readTree("{\"type\":\"WELCOME\"}");

		// when
		QueueMessage message = QueueMessage.of("notification.created", "member-1", payload);

		// then
		assertThat(message.topic()).isEqualTo("notification.created");
		assertThat(message.key()).isEqualTo("member-1");
		assertThat(message.payload().get("type").asText()).isEqualTo("WELCOME");
		assertThat(message.headers()).isEmpty();
		assertThat(message.messageId()).isNotBlank();
		assertThat(message.occurredAt()).isNotNull();
	}

	@Test
	@DisplayName("생성 후 원본 headers를 변경해도 메시지 내부 값은 유지된다")
	void constructor_defensivelyCopiesHeaders() throws Exception {
		// given
		JsonNode payload = MAPPER.readTree("{\"key\":\"value\"}");
		Map<String, String> headers = new HashMap<>();
		headers.put("source", "test");

		// when
		QueueMessage message = new QueueMessage(
			"group.member.joined",
			"group-1",
			payload,
			headers,
			null,
			null);

		headers.put("source", "changed");

		// then
		assertThat(message.payload().get("key").asText()).isEqualTo("value");
		assertThat(message.headers().get("source")).isEqualTo("test");
	}

	@Test
	@DisplayName("payload가 null이면 메시지를 생성할 수 없다")
	void constructor_nullPayload_throwsException() {
		// when & then
		assertThatThrownBy(() -> new QueueMessage("topic", "key", null, Map.of(), null, null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("topic이 비어 있으면 메시지를 생성할 수 없다")
	void constructor_blankTopic_throwsException() throws Exception {
		// given
		JsonNode payload = MAPPER.readTree("{}");

		// when & then
		assertThatThrownBy(() -> new QueueMessage(" ", "k", payload, Map.of(), null, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("topic은 필수");
	}
}
