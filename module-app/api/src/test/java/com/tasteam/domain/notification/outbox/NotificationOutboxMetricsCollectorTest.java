package com.tasteam.domain.notification.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](Notification) NotificationOutboxMetricsCollector 단위 테스트")
class NotificationOutboxMetricsCollectorTest {

	@Test
	@DisplayName("Outbox 요약값으로 pending/published/failed/retrying 게이지를 갱신한다")
	void refresh_updatesGaugeValues() {
		NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
		when(outboxService.summarize()).thenReturn(new NotificationOutboxSummary(10, 5, 2, 3, 4));
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		NotificationOutboxMetricsCollector collector = new NotificationOutboxMetricsCollector(outboxService,
			meterRegistry);

		collector.init();

		assertThat(meterRegistry.get("notification.outbox.pending").gauge().value()).isEqualTo(10.0);
		assertThat(meterRegistry.get("notification.outbox.published").gauge().value()).isEqualTo(5.0);
		assertThat(meterRegistry.get("notification.outbox.failed").gauge().value()).isEqualTo(2.0);
		assertThat(meterRegistry.get("notification.outbox.retrying").gauge().value()).isEqualTo(3.0);
	}
}
