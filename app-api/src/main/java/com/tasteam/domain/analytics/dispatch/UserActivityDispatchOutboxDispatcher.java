package com.tasteam.domain.analytics.dispatch;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
public class UserActivityDispatchOutboxDispatcher {

	private final UserActivityDispatchOutboxService outboxService;
	private final UserActivityDispatchSinkRegistry dispatchSinkRegistry;
	private final UserActivityDispatchCircuitBreaker circuitBreaker;
	private final AnalyticsDispatchProperties dispatchProperties;
	private final ObjectMapper objectMapper;
	@Nullable
	private final MeterRegistry meterRegistry;

	public UserActivityDispatchResult dispatchPendingPosthog() {
		return dispatchPending(UserActivityDispatchTarget.POSTHOG, dispatchProperties.getBatchSize());
	}

	public UserActivityDispatchResult dispatchPending(UserActivityDispatchTarget dispatchTarget, int limit) {
		if (!dispatchProperties.isEnabled()) {
			return new UserActivityDispatchResult(0, 0, 0, false);
		}
		if (!circuitBreaker.allowRequest()) {
			incrementCounter("analytics.user-activity.dispatch.circuit", "open", dispatchTarget);
			return new UserActivityDispatchResult(0, 0, 0, true);
		}

		UserActivityDispatchSink sink = dispatchSinkRegistry.getRequired(dispatchTarget);
		List<UserActivityDispatchOutboxEntry> candidates = outboxService.findDispatchCandidates(dispatchTarget, limit);
		int successCount = 0;
		int failedCount = 0;
		for (UserActivityDispatchOutboxEntry candidate : candidates) {
			if (!circuitBreaker.allowRequest()) {
				break;
			}

			ActivityEvent event = deserialize(candidate);
			if (event == null) {
				failedCount++;
				continue;
			}

			try {
				sink.dispatch(event);
				outboxService.markDispatched(candidate.id(), dispatchTarget);
				circuitBreaker.recordSuccess();
				successCount++;
			} catch (Exception ex) {
				outboxService.markFailed(candidate.id(), dispatchTarget, ex, retryBaseDelay(), retryMaxDelay());
				circuitBreaker.recordFailure();
				failedCount++;
				log.error("사용자 이벤트 dispatch에 실패했습니다. target={}, outboxId={}, eventId={}",
					dispatchTarget, candidate.id(), candidate.eventId(), ex);
			}
		}
		return new UserActivityDispatchResult(candidates.size(), successCount, failedCount, circuitBreaker.isOpen());
	}

	private ActivityEvent deserialize(UserActivityDispatchOutboxEntry candidate) {
		try {
			return objectMapper.readValue(candidate.payloadJson(), ActivityEvent.class);
		} catch (Exception ex) {
			outboxService.markFailed(candidate.id(), candidate.dispatchTarget(), ex, retryBaseDelay(), retryMaxDelay());
			circuitBreaker.recordFailure();
			log.error("사용자 이벤트 dispatch payload 역직렬화에 실패했습니다. outboxId={}, eventId={}",
				candidate.id(), candidate.eventId(), ex);
			return null;
		}
	}

	private Duration retryBaseDelay() {
		return dispatchProperties.getRetry().getBaseDelay();
	}

	private Duration retryMaxDelay() {
		return dispatchProperties.getRetry().getMaxDelay();
	}

	private void incrementCounter(String metricName, String state, UserActivityDispatchTarget dispatchTarget) {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.counter(metricName,
			"state", state,
			"target", dispatchTarget.name().toLowerCase())
			.increment();
	}
}
