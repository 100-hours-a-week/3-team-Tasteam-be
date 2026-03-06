package com.tasteam.domain.analytics.resilience;

import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.global.aop.ObservedOutbox;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivitySourceOutboxService {

	private final UserActivitySourceOutboxJdbcRepository outboxRepository;
	private final UserActivitySourceOutboxMetricsCollector metricsCollector;

	@Transactional
	public void enqueue(ActivityEvent event) {
		try {
			boolean inserted = outboxRepository.insertPendingIfAbsent(event);
			if (inserted) {
				metricsCollector.recordEnqueueResult("inserted");
				return;
			}
			metricsCollector.recordEnqueueResult("duplicate");
		} catch (Exception ex) {
			metricsCollector.recordEnqueueResult("fail");
			throw ex;
		}
	}

	@Transactional
	public void markPublished(String eventId) {
		outboxRepository.markPublished(eventId);
		metricsCollector.recordPublishResult("success");
	}

	@Transactional
	public void markFailed(String eventId, Throwable ex) {
		outboxRepository.markFailed(eventId, ex == null ? null : ex.getMessage());
		metricsCollector.recordPublishResult("fail");
		metricsCollector.recordRetryScheduled();
	}

	@Transactional(readOnly = true)
	public List<UserActivitySourceOutboxEntry> findReplayCandidates(int limit) {
		return outboxRepository.findReplayCandidates(limit, Instant.now());
	}

	@Transactional(readOnly = true)
	@ObservedOutbox(name = "analytics_source")
	public UserActivitySourceOutboxSummary summarize() {
		return outboxRepository.summarize();
	}

}
