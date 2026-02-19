package com.tasteam.domain.analytics.ingest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
@ConditionalOnProperty(prefix = "tasteam.analytics.ingest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientActivityIngestRateLimiter {

	private final int maxRequests;
	private final Duration window;
	private final Clock clock;
	private final Cache<String, WindowCounter> counterByKey;

	public ClientActivityIngestRateLimiter(AnalyticsIngestProperties properties) {
		this(
			properties.getRateLimit().validatedMaxRequests(),
			properties.getRateLimit().validatedWindow(),
			Clock.systemUTC());
	}

	ClientActivityIngestRateLimiter(int maxRequests, Duration window, Clock clock) {
		this.maxRequests = Math.max(1, maxRequests);
		this.window = sanitizeWindow(window);
		this.clock = clock == null ? Clock.systemUTC() : clock;
		this.counterByKey = Caffeine.newBuilder()
			.expireAfterAccess(this.window.multipliedBy(10))
			.maximumSize(100_000)
			.build();
	}

	public boolean tryAcquire(String key) {
		Objects.requireNonNull(key, "rate limit key는 null일 수 없습니다.");
		AtomicBoolean allowed = new AtomicBoolean(true);
		Instant now = Instant.now(clock);

		counterByKey.asMap().compute(key, (ignored, previous) -> {
			if (previous == null || isWindowExpired(previous, now)) {
				return WindowCounter.newWindow(now);
			}
			if (previous.requestCount() >= maxRequests) {
				allowed.set(false);
				return previous;
			}
			return previous.increment();
		});
		return allowed.get();
	}

	private boolean isWindowExpired(WindowCounter previous, Instant now) {
		return !now.isBefore(previous.windowStartedAt().plus(window));
	}

	private Duration sanitizeWindow(Duration candidate) {
		if (candidate == null || candidate.isZero() || candidate.isNegative()) {
			return Duration.ofMinutes(1);
		}
		return candidate;
	}

	private record WindowCounter(Instant windowStartedAt, int requestCount) {

		private static WindowCounter newWindow(Instant now) {
			return new WindowCounter(now, 1);
		}

		private WindowCounter increment() {
			return new WindowCounter(windowStartedAt, requestCount + 1);
		}
	}
}
