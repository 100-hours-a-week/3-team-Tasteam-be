package com.tasteam.domain.group.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("[유닛](Group) GroupType 단위 테스트")
class GroupTypeTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("official 값은 OFFICIAL로 역직렬화된다")
	void deserializesOfficial() throws Exception {
		GroupType type = objectMapper.readValue("\"official\"", GroupType.class);
		assertThat(type).isEqualTo(GroupType.OFFICIAL);
	}

	@Test
	@DisplayName("UNOFFICIAL 값은 UNOFFICIAL로 역직렬화된다")
	void deserializesUnofficial() throws Exception {
		GroupType type = objectMapper.readValue("\"UNOFFICIAL\"", GroupType.class);
		assertThat(type).isEqualTo(GroupType.UNOFFICIAL);
	}

	@Test
	@DisplayName("SCHOOL 별칭은 OFFICIAL로 역직렬화된다")
	void deserializesSchoolAliasToOfficial() throws Exception {
		GroupType type = objectMapper.readValue("\"SCHOOL\"", GroupType.class);
		assertThat(type).isEqualTo(GroupType.OFFICIAL);
	}

	@Test
	@DisplayName("알 수 없는 값이면 예외가 발생한다")
	void rejectsUnknownValues() {
		assertThatThrownBy(() -> objectMapper.readValue("\"COMMUNITY\"", GroupType.class))
			.hasRootCauseInstanceOf(IllegalArgumentException.class);
	}
}
