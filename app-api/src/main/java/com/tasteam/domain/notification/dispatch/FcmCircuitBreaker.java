package com.tasteam.domain.notification.dispatch;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FcmCircuitBreaker {

	private final int failureThreshold;
	private final Duration openDuration;
	private final Clock clock;

	private int consecutiveFailures;
	private Instant openedAt;

	@Autowired
	public FcmCircuitBreaker(
		@Value("${tasteam.notification.circuit-breaker.fcm.failure-threshold:5}")
		int failureThreshold,
		@Value("${tasteam.notification.circuit-breaker.fcm.open-duration-seconds:60}")
		int openDurationSeconds) {
		this(failureThreshold, Duration.ofSeconds(openDurationSeconds), Clock.systemUTC());
	}

	FcmCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
		this.failureThreshold = Math.max(1, failureThreshold);
		this.openDuration = openDuration == null || openDuration.isNegative() || openDuration.isZero()
			? Duration.ofMinutes(1) : openDuration;
		this.clock = clock == null ? Clock.systemUTC() : clock;
	}

	public synchronized boolean allowRequest() {
		return !isOpenInternal(Instant.now(clock));
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
}
