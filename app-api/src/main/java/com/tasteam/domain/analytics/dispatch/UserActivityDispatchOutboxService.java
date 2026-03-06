package com.tasteam.domain.analytics.dispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.global.aop.ObservedOutbox;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivityDispatchOutboxService {

	private final UserActivityDispatchOutboxJdbcRepository outboxRepository;
	private final UserActivityDispatchOutboxMetricsCollector metricsCollector;

	@Transactional
	public void enqueue(ActivityEvent event, UserActivityDispatchTarget dispatchTarget) {
		try {
			boolean inserted = outboxRepository.insertPendingIfAbsent(event, dispatchTarget);
			if (inserted) {
				metricsCollector.recordEnqueueResult("inserted", dispatchTarget);
				return;
			}
			metricsCollector.recordEnqueueResult("duplicate", dispatchTarget);
		} catch (Exception ex) {
			metricsCollector.recordEnqueueResult("fail", dispatchTarget);
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
		metricsCollector.recordExecuteResult("success", dispatchTarget);
	}

	@Transactional
	public void markFailed(
		long id,
		UserActivityDispatchTarget dispatchTarget,
		Throwable ex,
		Duration baseDelay,
		Duration maxDelay) {
		outboxRepository.markFailed(id, ex == null ? null : ex.getMessage(), baseDelay, maxDelay);
		metricsCollector.recordExecuteResult("fail", dispatchTarget);
		metricsCollector.recordRetryScheduled(dispatchTarget);
	}

	@ObservedOutbox(name = "analytics_dispatch")
	@Transactional(readOnly = true)
	public List<UserActivityDispatchOutboxSummary> summarizeByTarget() {
		return outboxRepository.summarizeByTarget();
	}
}
