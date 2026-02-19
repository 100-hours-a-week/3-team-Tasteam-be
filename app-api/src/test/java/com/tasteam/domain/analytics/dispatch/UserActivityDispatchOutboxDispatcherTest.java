package com.tasteam.domain.analytics.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;

@UnitTest
@DisplayName("사용자 이벤트 dispatch outbox 디스패처")
class UserActivityDispatchOutboxDispatcherTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	@Test
	@DisplayName("정상 payload는 sink로 전송하고 dispatch 완료로 마킹한다")
	void dispatchPending_dispatchesAndMarksSuccess() throws Exception {
		// given
		UserActivityDispatchOutboxService outboxService = mock(UserActivityDispatchOutboxService.class);
		UserActivityDispatchSinkRegistry sinkRegistry = mock(UserActivityDispatchSinkRegistry.class);
		UserActivityDispatchSink sink = mock(UserActivityDispatchSink.class);
		when(sinkRegistry.getRequired(UserActivityDispatchTarget.POSTHOG)).thenReturn(sink);
		AnalyticsDispatchProperties properties = dispatchProperties();
		UserActivityDispatchCircuitBreaker circuitBreaker = new UserActivityDispatchCircuitBreaker(
			3,
			Duration.ofMinutes(1),
			Clock.systemUTC());
		UserActivityDispatchOutboxDispatcher dispatcher = new UserActivityDispatchOutboxDispatcher(
			outboxService,
			sinkRegistry,
			circuitBreaker,
			properties,
			objectMapper,
			null);

		ActivityEvent event = sampleEvent("evt-1");
		UserActivityDispatchOutboxEntry entry = new UserActivityDispatchOutboxEntry(
			1L,
			"evt-1",
			UserActivityDispatchTarget.POSTHOG,
			objectMapper.writeValueAsString(event),
			UserActivityDispatchOutboxStatus.PENDING,
			0,
			null);
		when(outboxService.findDispatchCandidates(UserActivityDispatchTarget.POSTHOG, 100)).thenReturn(List.of(entry));

		// when
		UserActivityDispatchResult result = dispatcher.dispatchPending(UserActivityDispatchTarget.POSTHOG, 100);

		// then
		assertThat(result.processedCount()).isEqualTo(1);
		assertThat(result.successCount()).isEqualTo(1);
		assertThat(result.failedCount()).isZero();
		assertThat(result.circuitOpen()).isFalse();
		verify(sink).dispatch(any(ActivityEvent.class));
		verify(outboxService).markDispatched(1L, UserActivityDispatchTarget.POSTHOG);
	}

	@Test
	@DisplayName("payload 역직렬화가 실패하면 sink 호출 없이 실패 재시도를 예약한다")
	void dispatchPending_marksFailedWhenPayloadInvalid() {
		// given
		UserActivityDispatchOutboxService outboxService = mock(UserActivityDispatchOutboxService.class);
		UserActivityDispatchSinkRegistry sinkRegistry = mock(UserActivityDispatchSinkRegistry.class);
		UserActivityDispatchSink sink = mock(UserActivityDispatchSink.class);
		when(sinkRegistry.getRequired(UserActivityDispatchTarget.POSTHOG)).thenReturn(sink);
		AnalyticsDispatchProperties properties = dispatchProperties();
		UserActivityDispatchCircuitBreaker circuitBreaker = new UserActivityDispatchCircuitBreaker(
			3,
			Duration.ofMinutes(1),
			Clock.systemUTC());
		UserActivityDispatchOutboxDispatcher dispatcher = new UserActivityDispatchOutboxDispatcher(
			outboxService,
			sinkRegistry,
			circuitBreaker,
			properties,
			objectMapper,
			null);

		UserActivityDispatchOutboxEntry entry = new UserActivityDispatchOutboxEntry(
			9L,
			"evt-broken",
			UserActivityDispatchTarget.POSTHOG,
			"{\"eventId\":\"missing-required-fields\"}",
			UserActivityDispatchOutboxStatus.FAILED,
			3,
			Instant.now());
		when(outboxService.findDispatchCandidates(UserActivityDispatchTarget.POSTHOG, 100)).thenReturn(List.of(entry));

		// when
		UserActivityDispatchResult result = dispatcher.dispatchPending(UserActivityDispatchTarget.POSTHOG, 100);

		// then
		assertThat(result.processedCount()).isEqualTo(1);
		assertThat(result.successCount()).isZero();
		assertThat(result.failedCount()).isEqualTo(1);
		verify(sink, never()).dispatch(any(ActivityEvent.class));
		verify(outboxService).markFailed(
			eq(9L),
			eq(UserActivityDispatchTarget.POSTHOG),
			any(Throwable.class),
			eq(properties.getRetry().getBaseDelay()),
			eq(properties.getRetry().getMaxDelay()));
	}

	@Test
	@DisplayName("외부 sink 실패가 연속되면 서킷을 열고 추가 전송을 중단한다")
	void dispatchPending_opensCircuitWhenSinkKeepsFailing() throws Exception {
		// given
		UserActivityDispatchOutboxService outboxService = mock(UserActivityDispatchOutboxService.class);
		UserActivityDispatchSinkRegistry sinkRegistry = mock(UserActivityDispatchSinkRegistry.class);
		UserActivityDispatchSink sink = mock(UserActivityDispatchSink.class);
		when(sinkRegistry.getRequired(UserActivityDispatchTarget.POSTHOG)).thenReturn(sink);
		AnalyticsDispatchProperties properties = dispatchProperties();
		properties.getCircuit().setFailureThreshold(1);
		MutableClock clock = new MutableClock(Instant.parse("2026-02-19T00:00:00Z"));
		UserActivityDispatchCircuitBreaker circuitBreaker = new UserActivityDispatchCircuitBreaker(
			1,
			Duration.ofMinutes(1),
			clock);
		UserActivityDispatchOutboxDispatcher dispatcher = new UserActivityDispatchOutboxDispatcher(
			outboxService,
			sinkRegistry,
			circuitBreaker,
			properties,
			objectMapper,
			null);

		ActivityEvent event = sampleEvent("evt-fail");
		UserActivityDispatchOutboxEntry first = new UserActivityDispatchOutboxEntry(
			1L,
			"evt-fail-1",
			UserActivityDispatchTarget.POSTHOG,
			objectMapper.writeValueAsString(event),
			UserActivityDispatchOutboxStatus.PENDING,
			0,
			null);
		UserActivityDispatchOutboxEntry second = new UserActivityDispatchOutboxEntry(
			2L,
			"evt-fail-2",
			UserActivityDispatchTarget.POSTHOG,
			objectMapper.writeValueAsString(event),
			UserActivityDispatchOutboxStatus.PENDING,
			0,
			null);
		when(outboxService.findDispatchCandidates(UserActivityDispatchTarget.POSTHOG, 100))
			.thenReturn(List.of(first, second));
		org.mockito.Mockito.doThrow(new IllegalStateException("posthog down"))
			.when(sink)
			.dispatch(any(ActivityEvent.class));

		// when
		UserActivityDispatchResult result = dispatcher.dispatchPending(UserActivityDispatchTarget.POSTHOG, 100);

		// then
		assertThat(result.processedCount()).isEqualTo(2);
		assertThat(result.successCount()).isZero();
		assertThat(result.failedCount()).isEqualTo(1);
		assertThat(result.circuitOpen()).isTrue();
		verify(sink).dispatch(any(ActivityEvent.class));
		verify(outboxService).markFailed(
			eq(1L),
			eq(UserActivityDispatchTarget.POSTHOG),
			any(Throwable.class),
			eq(properties.getRetry().getBaseDelay()),
			eq(properties.getRetry().getMaxDelay()));
		verify(outboxService, never()).markDispatched(eq(2L), eq(UserActivityDispatchTarget.POSTHOG));
	}

	@Test
	@DisplayName("서킷이 이미 열려 있으면 후보 조회 없이 즉시 종료한다")
	void dispatchPending_skipsWhenCircuitAlreadyOpen() {
		// given
		UserActivityDispatchOutboxService outboxService = mock(UserActivityDispatchOutboxService.class);
		UserActivityDispatchSinkRegistry sinkRegistry = mock(UserActivityDispatchSinkRegistry.class);
		AnalyticsDispatchProperties properties = dispatchProperties();
		MutableClock clock = new MutableClock(Instant.parse("2026-02-19T00:00:00Z"));
		UserActivityDispatchCircuitBreaker circuitBreaker = new UserActivityDispatchCircuitBreaker(
			1,
			Duration.ofMinutes(1),
			clock);
		circuitBreaker.recordFailure();
		UserActivityDispatchOutboxDispatcher dispatcher = new UserActivityDispatchOutboxDispatcher(
			outboxService,
			sinkRegistry,
			circuitBreaker,
			properties,
			objectMapper,
			null);

		// when
		UserActivityDispatchResult result = dispatcher.dispatchPending(UserActivityDispatchTarget.POSTHOG, 100);

		// then
		assertThat(result.processedCount()).isZero();
		assertThat(result.successCount()).isZero();
		assertThat(result.failedCount()).isZero();
		assertThat(result.circuitOpen()).isTrue();
		verifyNoInteractions(outboxService, sinkRegistry);
	}

	private AnalyticsDispatchProperties dispatchProperties() {
		AnalyticsDispatchProperties properties = new AnalyticsDispatchProperties();
		properties.setEnabled(true);
		properties.setBatchSize(100);
		properties.getRetry().setBaseDelay(Duration.ofSeconds(10));
		properties.getRetry().setMaxDelay(Duration.ofMinutes(10));
		properties.getCircuit().setFailureThreshold(3);
		properties.getCircuit().setOpenDuration(Duration.ofMinutes(1));
		return properties;
	}

	private ActivityEvent sampleEvent(String eventId) {
		return new ActivityEvent(
			eventId,
			"review.created",
			"v1",
			Instant.parse("2026-02-19T00:00:00Z"),
			10L,
			null,
			Map.of("restaurantId", 1L));
	}

	private static final class MutableClock extends Clock {

		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return current;
		}
	}
}
