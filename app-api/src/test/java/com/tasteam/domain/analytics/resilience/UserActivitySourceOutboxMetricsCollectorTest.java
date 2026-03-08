package com.tasteam.domain.analytics.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](UserActivity) UserActivitySourceOutboxMetricsCollector 단위 테스트")
class UserActivitySourceOutboxMetricsCollectorTest {

	@Test
	@DisplayName("Source Outbox 요약값으로 pending/failed 게이지를 갱신한다")
	void refresh_updatesGaugeValues() {
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		when(outboxService.summarize()).thenReturn(new UserActivitySourceOutboxSummary(7, 2, 100, 5));
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		UserActivitySourceOutboxMetricsCollector collector = new UserActivitySourceOutboxMetricsCollector(
			outboxService,
			meterRegistry);

		collector.init();

		assertThat(meterRegistry.get("analytics.user-activity.source.outbox.pending").gauge().value()).isEqualTo(7.0);
		assertThat(meterRegistry.get("analytics.user-activity.source.outbox.failed").gauge().value()).isEqualTo(2.0);
	}
}
