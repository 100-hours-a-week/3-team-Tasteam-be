package com.tasteam.domain.analytics.resilience;

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
public class UserActivitySourceOutboxService {

	private final UserActivitySourceOutboxJdbcRepository outboxRepository;
	@Nullable
	private final MeterRegistry meterRegistry;

	@Transactional
	public void enqueue(ActivityEvent event) {
		try {
			boolean inserted = outboxRepository.insertPendingIfAbsent(event);
			if (inserted) {
				incrementCounter("analytics.user-activity.outbox.enqueue", "inserted");
				return;
			}
			incrementCounter("analytics.user-activity.outbox.enqueue", "duplicate");
		} catch (Exception ex) {
			incrementCounter("analytics.user-activity.outbox.enqueue", "fail");
			throw ex;
		}
	}

	@Transactional
	public void markPublished(String eventId) {
		outboxRepository.markPublished(eventId);
		incrementCounter("analytics.user-activity.outbox.publish", "success");
	}

	@Transactional
	public void markFailed(String eventId, Throwable ex) {
		outboxRepository.markFailed(eventId, ex == null ? null : ex.getMessage());
		incrementCounter("analytics.user-activity.outbox.publish", "fail");
		incrementCounter("analytics.user-activity.outbox.retry", "scheduled");
	}

	@Transactional(readOnly = true)
	public List<UserActivitySourceOutboxEntry> findReplayCandidates(int limit) {
		return outboxRepository.findReplayCandidates(limit, Instant.now());
	}

	@Transactional(readOnly = true)
	public UserActivitySourceOutboxSummary summarize() {
		return outboxRepository.summarize();
	}

	private void incrementCounter(String metricName, String result) {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.counter(metricName, "result", result).increment();
	}
}
