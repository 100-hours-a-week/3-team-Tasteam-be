package com.tasteam.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.tasteam.config.annotation.UnitTest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("RedisRateLimiter 단위 테스트")
class RedisRateLimiterTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Test
	@DisplayName("Lua 결과를 RateLimitResult로 매핑한다")
	void checkMailSend_resultMapping_success() {
		RateLimitPolicy policy = new RateLimitPolicy();
		RateLimitKeyFactory keyFactory = new RateLimitKeyFactory(policy);
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate, policy, keyFactory, meterRegistry);

		given(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any()))
			.willReturn(List.of(0L, "RATE_LIMIT_IP_1M", 17L));

		RateLimitResult result = limiter.checkMailSend(new RateLimitRequest("user@example.com", "10.0.0.1", 1L));

		assertThat(result.allowed()).isFalse();
		assertThat(result.reason()).isEqualTo(RateLimitReason.RATE_LIMIT_IP_1M);
		assertThat(result.retryAfterSeconds()).isEqualTo(17L);
		assertThat(meterRegistry.find("redis_eval_latency").tag("result", "success").timer()).isNotNull();
	}

	@Test
	@DisplayName("Redis 예외 발생 시 fail-closed 결과와 에러 메트릭을 반환한다")
	void checkMailSend_redisError_failClosed() {
		RateLimitPolicy policy = new RateLimitPolicy();
		RateLimitKeyFactory keyFactory = new RateLimitKeyFactory(policy);
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate, policy, keyFactory, meterRegistry);

		given(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any()))
			.willThrow(new RedisSystemException("redis fail", new RuntimeException("boom")));

		RateLimitResult result = limiter.checkMailSend(new RateLimitRequest("user@example.com", "10.0.0.1", 1L));

		assertThat(result.allowed()).isFalse();
		assertThat(result.reason()).isEqualTo(RateLimitReason.RATE_LIMITER_UNAVAILABLE);
		assertThat(meterRegistry.find("redis_errors_count").counter()).isNotNull();
		assertThat(meterRegistry.find("redis_errors_count").counter().count()).isEqualTo(1.0d);
		assertThat(meterRegistry.find("redis_eval_latency").tag("result", "error").timer()).isNotNull();
	}
}
