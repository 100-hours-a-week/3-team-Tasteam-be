package com.tasteam.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("RateLimitKeyFactory 단위 테스트")
class RateLimitKeyFactoryTest {

	@Test
	@DisplayName("이메일은 trim, lowercase, 내부 공백 제거로 정규화된다")
	void normalizeEmail_success() {
		RateLimitPolicy policy = new RateLimitPolicy();
		RateLimitKeyFactory factory = new RateLimitKeyFactory(policy);

		String normalized = factory.normalizeEmail("  User .Name+tag@Example.COM  ");

		assertThat(normalized).isEqualTo("user.name+tag@example.com");
	}

	@Test
	@DisplayName("키 포맷이 정책 prefix/action 기준으로 생성된다")
	void keyFormat_success() {
		RateLimitPolicy policy = new RateLimitPolicy();
		policy.setPrefix("rl:mail");
		policy.setAction("send");
		RateLimitKeyFactory factory = new RateLimitKeyFactory(policy);

		assertThat(factory.email1mKey("a@example.com")).isEqualTo("rl:mail:send:email:1m:a@example.com");
		assertThat(factory.ip1mKey("10.0.0.1")).isEqualTo("rl:mail:send:ip:1m:10.0.0.1");
		assertThat(factory.user1mKey(7L)).isEqualTo("rl:mail:send:user:1m:7");
		assertThat(factory.emailBlockKey("a@example.com")).isEqualTo("rl:mail:block:email:a@example.com");
	}

	@Test
	@DisplayName("일일 키는 timezone 기준 yyyyMMdd를 사용한다")
	void email1dKey_timezoneBoundary_success() {
		RateLimitPolicy policy = new RateLimitPolicy();
		policy.setTimezone("Asia/Seoul");
		RateLimitKeyFactory factory = new RateLimitKeyFactory(policy);

		ZonedDateTime utc = ZonedDateTime.of(2026, 3, 1, 15, 10, 0, 0, ZoneId.of("UTC"));
		String key = factory.email1dKey("a@example.com", utc);

		assertThat(key).isEqualTo("rl:mail:send:email:1d:a@example.com:20260302");
	}
}
