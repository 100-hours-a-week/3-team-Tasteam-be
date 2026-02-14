package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("메시지큐 제공자 타입 매핑")
class MessageQueueProviderTypeTest {

	@Test
	@DisplayName("지원하는 별칭을 입력하면 올바른 제공자 타입으로 변환한다")
	void from_mapsAliasesToProviderType() {
		// given & when & then
		assertThat(MessageQueueProviderType.from("none")).isEqualTo(MessageQueueProviderType.NONE);
		assertThat(MessageQueueProviderType.from("redis-stream")).isEqualTo(MessageQueueProviderType.REDIS_STREAM);
		assertThat(MessageQueueProviderType.from("redis_stream")).isEqualTo(MessageQueueProviderType.REDIS_STREAM);
		assertThat(MessageQueueProviderType.from("kafka")).isEqualTo(MessageQueueProviderType.KAFKA);
	}

	@Test
	@DisplayName("지원하지 않는 제공자 값을 입력하면 예외가 발생한다")
	void from_unknownValue_throwsException() {
		// when & then
		assertThatThrownBy(() -> MessageQueueProviderType.from("rabbitmq"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("지원하지 않는 메시지큐 provider");
	}
}
