package com.tasteam.domain.group.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class GroupTypeTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void deserializesOfficial() throws Exception {
		GroupType type = objectMapper.readValue("\"official\"", GroupType.class);
		assertThat(type).isEqualTo(GroupType.OFFICIAL);
	}

	@Test
	void deserializesUnofficial() throws Exception {
		GroupType type = objectMapper.readValue("\"UNOFFICIAL\"", GroupType.class);
		assertThat(type).isEqualTo(GroupType.UNOFFICIAL);
	}

	@Test
	void deserializesSchoolAliasToOfficial() throws Exception {
		GroupType type = objectMapper.readValue("\"SCHOOL\"", GroupType.class);
		assertThat(type).isEqualTo(GroupType.OFFICIAL);
	}

	@Test
	void rejectsUnknownValues() {
		assertThatThrownBy(() -> objectMapper.readValue("\"COMMUNITY\"", GroupType.class))
			.hasRootCauseInstanceOf(IllegalArgumentException.class);
	}
}
