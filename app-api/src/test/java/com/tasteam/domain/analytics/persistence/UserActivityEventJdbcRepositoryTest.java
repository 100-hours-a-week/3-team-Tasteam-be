package com.tasteam.domain.analytics.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;

@UnitTest
@DisplayName("사용자 이벤트 JDBC 저장소")
class UserActivityEventJdbcRepositoryTest {

	@Test
	@DisplayName("이벤트를 저장하면 ON CONFLICT DO NOTHING 기반 insert를 수행한다")
	void insertIgnoreDuplicate_executesInsertQuery() {
		// given
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(1);

		UserActivityEventJdbcRepository repository = new UserActivityEventJdbcRepository(
			jdbcTemplate,
			JsonMapper.builder().findAndAddModules().build());

		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"review.created",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			11L,
			null,
			Map.of("restaurantId", 88L));

		// when
		boolean inserted = repository.insertIgnoreDuplicate(event);

		// then
		assertThat(inserted).isTrue();
		verify(jdbcTemplate).update(any(String.class), any(MapSqlParameterSource.class));
	}

	@Test
	@DisplayName("중복 이벤트면 insert 결과가 0으로 반환된다")
	void insertIgnoreDuplicate_returnsFalseWhenDuplicate() {
		// given
		NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		when(jdbcTemplate.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(0);

		UserActivityEventJdbcRepository repository = new UserActivityEventJdbcRepository(
			jdbcTemplate,
			JsonMapper.builder().findAndAddModules().build());

		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"group.joined",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			11L,
			null,
			Map.of("groupId", 77L));

		// when
		boolean inserted = repository.insertIgnoreDuplicate(event);

		// then
		assertThat(inserted).isFalse();
	}
}
