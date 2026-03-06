package com.tasteam.domain.analytics.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](UserActivity) UserActivityReplayRunner 단위 테스트")
class UserActivityReplayRunnerTest {

	@Test
	@DisplayName("재처리 배치 결과를 replay processed/latency 메트릭으로 기록한다")
	void runPendingReplay_recordsReplayMetrics() {
		UserActivityReplayService replayService = mock(UserActivityReplayService.class);
		when(replayService.replayPending(200)).thenReturn(new UserActivityReplayResult(4, 3, 1));
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		UserActivityReplayRunner runner = new UserActivityReplayRunner(replayService, meterRegistry);

		UserActivityReplayResult result = runner.runPendingReplay(200);

		verify(replayService).replayPending(200);
		assertThat(result).isNotNull();
		assertThat(result.processedCount()).isEqualTo(4);
		assertThat(result.successCount()).isEqualTo(3);
		assertThat(result.failedCount()).isEqualTo(1);
		assertThat(meterRegistry.get("analytics.user-activity.replay.processed")
			.tag("result", "success")
			.counter()
			.count()).isEqualTo(3.0);
		assertThat(meterRegistry.get("analytics.user-activity.replay.processed")
			.tag("result", "fail")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(meterRegistry.get("analytics.user-activity.replay.batch.duration")
			.timer()
			.count()).isEqualTo(1L);
	}
}
