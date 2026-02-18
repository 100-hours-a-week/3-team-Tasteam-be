package com.tasteam.domain.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivityEventMapper;

@UnitTest
@DisplayName("사용자 이벤트 매퍼 레지스트리")
class ActivityEventMapperRegistryTest {

	@Test
	@DisplayName("이벤트 타입에 매칭되는 매퍼가 있으면 조회된다")
	void findMapper_returnsMapperWhenRegistered() {
		// given
		ActivityEventMapperRegistry registry = new ActivityEventMapperRegistry(List.of(new SampleEventMapper()));

		// when
		ActivityEventMapper<Object> mapper = registry.findMapper(SampleEvent.class).orElse(null);

		// then
		assertThat(mapper).isNotNull();
		assertThat(mapper.sourceType()).isEqualTo(SampleEvent.class);
	}

	@Test
	@DisplayName("등록되지 않은 이벤트 타입이면 빈 결과를 반환한다")
	void findMapper_returnsEmptyWhenNotRegistered() {
		// given
		ActivityEventMapperRegistry registry = new ActivityEventMapperRegistry(List.of(new SampleEventMapper()));

		// when
		boolean exists = registry.findMapper(UnknownEvent.class).isPresent();

		// then
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("동일 이벤트 타입 매퍼가 중복되면 레지스트리 생성에 실패한다")
	void constructor_throwsWhenDuplicateMapperRegistered() {
		// given
		List<ActivityEventMapper<?>> duplicatedMappers = List.of(
			new SampleEventMapper(),
			new SampleEventMapper());

		// when & then
		assertThatThrownBy(() -> new ActivityEventMapperRegistry(duplicatedMappers))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("중복 등록");
	}

	private record SampleEvent(long value) {
	}

	private record UnknownEvent(long value) {
	}

	private static class SampleEventMapper implements ActivityEventMapper<SampleEvent> {

		@Override
		public Class<SampleEvent> sourceType() {
			return SampleEvent.class;
		}

		@Override
		public ActivityEvent map(SampleEvent event) {
			return new ActivityEvent(
				"event-1",
				"sample.event",
				"v1",
				Instant.parse("2026-02-18T00:00:00Z"),
				null,
				null,
				Map.of("value", event.value()));
		}
	}
}
