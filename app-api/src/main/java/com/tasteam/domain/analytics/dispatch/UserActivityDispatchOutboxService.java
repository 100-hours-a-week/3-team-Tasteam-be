package com.tasteam.domain.analytics.dispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.global.aop.ObservedOutbox;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivityDispatchOutboxService {

	private final UserActivityDispatchOutboxJdbcRepository outboxRepository;
	private final ObjectProvider<UserActivityDispatchOutboxMetricsCollector> metricsCollectorProvider;

	@Transactional
	public void enqueue(ActivityEvent event, UserActivityDispatchTarget dispatchTarget) {
		try {
			boolean inserted = outboxRepository.insertPendingIfAbsent(event, dispatchTarget);
			if (inserted) {
				recordEnqueueResult("inserted", dispatchTarget);
				return;
			}
			recordEnqueueResult("duplicate", dispatchTarget);
		} catch (Exception ex) {
			recordEnqueueResult("fail", dispatchTarget);
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
		recordExecuteResult("success", dispatchTarget);
	}

	@Transactional
	public void markFailed(
		long id,
		UserActivityDispatchTarget dispatchTarget,
		Throwable ex,
		Duration baseDelay,
		Duration maxDelay) {
		outboxRepository.markFailed(id, ex == null ? null : ex.getMessage(), baseDelay, maxDelay);
		recordExecuteResult("fail", dispatchTarget);
		recordRetryScheduled(dispatchTarget);
	}

	private void recordEnqueueResult(String result, UserActivityDispatchTarget dispatchTarget) {
		UserActivityDispatchOutboxMetricsCollector metricsCollector = metricsCollector();
		if (metricsCollector != null) {
			metricsCollector.recordEnqueueResult(result, dispatchTarget);
		}
	}

	private void recordExecuteResult(String result, UserActivityDispatchTarget dispatchTarget) {
		UserActivityDispatchOutboxMetricsCollector metricsCollector = metricsCollector();
		if (metricsCollector != null) {
			metricsCollector.recordExecuteResult(result, dispatchTarget);
		}
	}

	private void recordRetryScheduled(UserActivityDispatchTarget dispatchTarget) {
		UserActivityDispatchOutboxMetricsCollector metricsCollector = metricsCollector();
		if (metricsCollector != null) {
			metricsCollector.recordRetryScheduled(dispatchTarget);
		}
	}

	@Nullable
	private UserActivityDispatchOutboxMetricsCollector metricsCollector() {
		return metricsCollectorProvider.getIfAvailable();
	}

	@ObservedOutbox(name = "analytics_dispatch")
	@Transactional(readOnly = true)
	public List<UserActivityDispatchOutboxSummary> summarizeByTarget() {
		return outboxRepository.summarizeByTarget();
	}
}
