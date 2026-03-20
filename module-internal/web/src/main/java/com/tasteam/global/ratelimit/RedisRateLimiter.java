package com.tasteam.global.ratelimit;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

	private static final String SCRIPT_PATH = "redis/lua/mail_rate_limit.lua";

	private final StringRedisTemplate redisTemplate;
	private final RateLimitPolicy policy;
	private final RateLimitKeyFactory keyFactory;
	private final MeterRegistry meterRegistry;

	private volatile DefaultRedisScript<List> script;

	public RateLimitResult checkMailSend(RateLimitRequest request) {
		if (!policy.isEnabled()) {
			return RateLimitResult.allow();
		}

		String normalizedEmail = keyFactory.normalizeEmail(request.email());
		String normalizedIp = keyFactory.normalizeIp(request.ip());
		ZonedDateTime now = ZonedDateTime.now(policy.zoneId());

		List<String> keys = List.of(
			keyFactory.email1mKey(normalizedEmail),
			keyFactory.ip1mKey(normalizedIp),
			keyFactory.user1mKey(request.userId()),
			keyFactory.email1dKey(normalizedEmail, now),
			keyFactory.emailBlockKey(normalizedEmail));

		Object[] args = {
			String.valueOf(Instant.now().getEpochSecond()),
			String.valueOf(policy.getEmail1m().validatedLimit()),
			String.valueOf(policy.getIp1m().validatedLimit()),
			String.valueOf(policy.getUser1m().validatedLimit()),
			String.valueOf(policy.getEmail1d().validatedLimit()),
			String.valueOf(policy.getEmail1m().validatedTtlSeconds()),
			String.valueOf(policy.getEmail1d().validatedTtlSeconds()),
			String.valueOf(policy.blockTtlSeconds())
		};

		Timer.Sample sample = Timer.start(meterRegistry);
		String result = "success";
		try {
			List<?> scriptResult = redisTemplate.execute(getScript(), keys, args);
			return parseResult(scriptResult);
		} catch (RedisSystemException e) {
			result = "error";
			meterRegistry.counter("redis_errors_count").increment();
			return new RateLimitResult(false, RateLimitReason.RATE_LIMITER_UNAVAILABLE, 0L);
		} catch (Exception e) {
			result = "error";
			meterRegistry.counter("redis_errors_count").increment();
			return new RateLimitResult(false, RateLimitReason.RATE_LIMITER_UNAVAILABLE, 0L);
		} finally {
			sample.stop(meterRegistry.timer("redis_eval_latency", "result", result));
		}
	}

	private DefaultRedisScript<List> getScript() {
		if (script == null) {
			synchronized (this) {
				if (script == null) {
					DefaultRedisScript<List> newScript = new DefaultRedisScript<>();
					newScript.setLocation(new ClassPathResource(SCRIPT_PATH));
					newScript.setResultType(List.class);
					script = newScript;
				}
			}
		}
		return script;
	}

	private RateLimitResult parseResult(List<?> raw) {
		if (raw == null || raw.size() < 3) {
			return new RateLimitResult(false, RateLimitReason.RATE_LIMITER_UNAVAILABLE, 0L);
		}

		boolean allowed = toLong(raw.get(0)) == 1L;
		RateLimitReason reason = RateLimitReason.from(String.valueOf(raw.get(1)));
		long retryAfterSeconds = Math.max(0L, toLong(raw.get(2)));
		return new RateLimitResult(allowed, reason, retryAfterSeconds);
	}

	private long toLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
