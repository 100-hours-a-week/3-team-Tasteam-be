package com.tasteam.domain.analytics.dispatch;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
public class UserActivityDispatchCircuitBreaker {

	private final int failureThreshold;
	private final Duration openDuration;
	private final Clock clock;

	private int consecutiveFailures;
	private Instant openedAt;

	public UserActivityDispatchCircuitBreaker(AnalyticsDispatchProperties properties) {
		this(
			properties.getCircuit().getFailureThreshold(),
			properties.getCircuit().getOpenDuration(),
			Clock.systemUTC());
	}

	UserActivityDispatchCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
		this.failureThreshold = Math.max(1, failureThreshold);
		this.openDuration = sanitizeOpenDuration(openDuration);
		this.clock = clock == null ? Clock.systemUTC() : clock;
	}

	public synchronized boolean allowRequest() {
		Instant now = Instant.now(clock);
		if (!isOpenInternal(now)) {
			return true;
		}
		return false;
	}

	public synchronized void recordSuccess() {
		consecutiveFailures = 0;
		openedAt = null;
	}

	public synchronized void recordFailure() {
		Instant now = Instant.now(clock);
		if (isOpenInternal(now)) {
			return;
		}
		consecutiveFailures++;
		if (consecutiveFailures >= failureThreshold) {
			openedAt = now;
		}
	}

	public synchronized boolean isOpen() {
		return isOpenInternal(Instant.now(clock));
	}

	private boolean isOpenInternal(Instant now) {
		if (openedAt == null) {
			return false;
		}
		if (!now.isBefore(openedAt.plus(openDuration))) {
			openedAt = null;
			consecutiveFailures = 0;
			return false;
		}
		return true;
	}

	private Duration sanitizeOpenDuration(Duration duration) {
		if (duration == null || duration.isNegative() || duration.isZero()) {
			return Duration.ofMinutes(1);
		}
		return duration;
	}
}
