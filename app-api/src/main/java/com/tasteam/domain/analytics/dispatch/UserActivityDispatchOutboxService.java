package com.tasteam.domain.analytics.dispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.analytics.api.ActivityEvent;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivityDispatchOutboxService {

	private final UserActivityDispatchOutboxJdbcRepository outboxRepository;
	@Nullable
	private final MeterRegistry meterRegistry;

	@Transactional
	public void enqueue(ActivityEvent event, UserActivityDispatchTarget dispatchTarget) {
		try {
			boolean inserted = outboxRepository.insertPendingIfAbsent(event, dispatchTarget);
			if (inserted) {
				incrementCounter("analytics.user-activity.dispatch.enqueue", "inserted", dispatchTarget);
				return;
			}
			incrementCounter("analytics.user-activity.dispatch.enqueue", "duplicate", dispatchTarget);
		} catch (Exception ex) {
			incrementCounter("analytics.user-activity.dispatch.enqueue", "fail", dispatchTarget);
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public List<UserActivityDispatchOutboxEntry> findDispatchCandidates(UserActivityDispatchTarget dispatchTarget,
		int limit) {
		return outboxRepository.findDispatchCandidates(dispatchTarget, limit, Instant.now());
	}

	@Transactional
	public void markDispatched(long id, UserActivityDispatchTarget dispatchTarget) {
		outboxRepository.markDispatched(id);
		incrementCounter("analytics.user-activity.dispatch.execute", "success", dispatchTarget);
	}

	@Transactional
	public void markFailed(
		long id,
		UserActivityDispatchTarget dispatchTarget,
		Throwable ex,
		Duration baseDelay,
		Duration maxDelay) {
		outboxRepository.markFailed(id, ex == null ? null : ex.getMessage(), baseDelay, maxDelay);
		incrementCounter("analytics.user-activity.dispatch.execute", "fail", dispatchTarget);
		incrementCounter("analytics.user-activity.dispatch.retry", "scheduled", dispatchTarget);
	}

	private void incrementCounter(String metricName, String result, UserActivityDispatchTarget dispatchTarget) {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.counter(metricName,
			"result", result,
			"target", dispatchTarget.name().toLowerCase())
			.increment();
	}
}
