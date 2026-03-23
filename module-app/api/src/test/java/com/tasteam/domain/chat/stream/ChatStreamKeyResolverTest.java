package com.tasteam.domain.chat.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Chat) ChatStreamKeyResolver 단위 테스트")
class ChatStreamKeyResolverTest {

	private final ChatStreamKeyResolver keyResolver = new ChatStreamKeyResolver();

	@Test
	@DisplayName("파티션 키는 chat:partition:{id} 형식으로 생성된다")
	void partitionStreamKey_success() {
		assertThat(keyResolver.partitionStreamKey(7)).isEqualTo("chat:partition:7");
	}

	@Test
	@DisplayName("채팅방 ID는 floorMod로 파티션을 계산한다")
	void resolvePartition_success() {
		assertThat(keyResolver.resolvePartition(123L, 16)).isEqualTo(11);
		assertThat(keyResolver.resolvePartition(-1L, 16)).isEqualTo(15);
	}

	@Test
	@DisplayName("partitionCount가 0 이하이면 예외가 발생한다")
	void resolvePartition_invalidPartitionCount() {
		assertThatThrownBy(() -> keyResolver.resolvePartition(1L, 0))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
