package com.tasteam.domain.analytics.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](UserActivity) UserActivityDispatchOutboxMetricsCollector 단위 테스트")
class UserActivityDispatchOutboxMetricsCollectorTest {

	@Test
	@DisplayName("Dispatch Outbox 요약값으로 target별 pending/failed 게이지를 갱신한다")
	void refresh_updatesGaugeValues() {
		UserActivityDispatchOutboxService outboxService = mock(UserActivityDispatchOutboxService.class);
		when(outboxService.summarizeByTarget())
			.thenReturn(
				List.of(new UserActivityDispatchOutboxSummary(UserActivityDispatchTarget.POSTHOG, 11, 3, 20, 4)));
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		UserActivityDispatchOutboxMetricsCollector collector = new UserActivityDispatchOutboxMetricsCollector(
			outboxService,
			meterRegistry);

		collector.init();

		assertThat(meterRegistry.get("analytics.user-activity.dispatch.outbox.pending")
			.tag("target", "posthog")
			.gauge()
			.value()).isEqualTo(11.0);
		assertThat(meterRegistry.get("analytics.user-activity.dispatch.outbox.failed")
			.tag("target", "posthog")
			.gauge()
			.value()).isEqualTo(3.0);
	}
}
