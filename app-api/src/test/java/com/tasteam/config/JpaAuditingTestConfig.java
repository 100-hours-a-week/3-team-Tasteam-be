package com.tasteam.config;

import java.time.Instant;
import java.util.Optional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 테스트에서 JPA Auditing(@CreatedDate, @LastModifiedDate 등)에
 * 고정된 시간을 주입하기 위한 설정
 * - 항상 동일한 Instant를 반환하는 DateTimeProvider를 등록해
 *   감사 필드에 대한 테스트를 안정적으로 수행할 수 있다.
 */
@TestConfiguration
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
public class JpaAuditingTestConfig {

	private static final Instant FIXED_TEST_TIME = Instant.parse("2025-01-01T00:00:00Z");

	@Bean
	public TestClock testClock() {
		return new TestClock();
	}

	@Bean
	public DateTimeProvider testDateTimeProvider() {
		return () -> Optional.of(FIXED_TEST_TIME);
	}

	public static class TestClock {
		private Instant fixedInstant = FIXED_TEST_TIME;

		public Instant getFixedInstant() {
			return fixedInstant;
		}

		public void setFixedInstant(Instant instant) {
			this.fixedInstant = instant;
		}

		public void reset() {
			this.fixedInstant = FIXED_TEST_TIME;
		}
	}
}
