package com.tasteam.domain.analytics.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivityEventMapper;
import com.tasteam.domain.analytics.api.ActivitySink;

@UnitTest
@DisplayName("사용자 이벤트 오케스트레이터")
class ActivityEventOrchestratorTest {

	@Test
	@DisplayName("지원되는 도메인 이벤트를 수신하면 모든 sink에 전달한다")
	void handleDomainEvent_dispatchesToEverySink() {
		// given
		ActivitySink firstSink = mock(ActivitySink.class);
		ActivitySink secondSink = mock(ActivitySink.class);
		ActivityEventOrchestrator orchestrator = new ActivityEventOrchestrator(
			new ActivityEventMapperRegistry(List.of(new SampleEventMapper())),
			List.of(firstSink, secondSink));
		SampleEvent domainEvent = new SampleEvent(10L);

		// when
		orchestrator.handleDomainEvent(domainEvent);

		// then
		verify(firstSink).sink(any(ActivityEvent.class));
		verify(secondSink).sink(any(ActivityEvent.class));
	}

	@Test
	@DisplayName("일부 sink에서 실패해도 나머지 sink 전달은 계속된다")
	void handleDomainEvent_continuesWhenSinkFails() {
		// given
		ActivitySink failedSink = mock(ActivitySink.class);
		ActivitySink healthySink = mock(ActivitySink.class);
		org.mockito.Mockito.doThrow(new IllegalStateException("sink failure"))
			.when(failedSink)
			.sink(any(ActivityEvent.class));

		ActivityEventOrchestrator orchestrator = new ActivityEventOrchestrator(
			new ActivityEventMapperRegistry(List.of(new SampleEventMapper())),
			List.of(failedSink, healthySink));

		// when
		orchestrator.handleDomainEvent(new SampleEvent(20L));

		// then
		verify(failedSink, times(1)).sink(any(ActivityEvent.class));
		verify(healthySink, times(1)).sink(any(ActivityEvent.class));
	}

	@Test
	@DisplayName("지원 매퍼가 없는 이벤트는 sink에 전달하지 않는다")
	void handleDomainEvent_skipsWhenNoMapperExists() {
		// given
		ActivitySink sink = mock(ActivitySink.class);
		ActivityEventOrchestrator orchestrator = new ActivityEventOrchestrator(
			new ActivityEventMapperRegistry(List.of(new SampleEventMapper())),
			List.of(sink));

		// when
		orchestrator.handleDomainEvent(new UnknownEvent(1L));

		// then
		verifyNoInteractions(sink);
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
