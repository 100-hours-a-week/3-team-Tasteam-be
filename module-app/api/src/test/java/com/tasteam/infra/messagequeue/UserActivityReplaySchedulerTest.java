package com.tasteam.infra.messagequeue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.resilience.UserActivityReplayMetricsCollector;
import com.tasteam.domain.analytics.resilience.UserActivityReplayResult;
import com.tasteam.domain.analytics.resilience.UserActivityReplayRunner;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](UserActivity) UserActivityReplayScheduler 단위 테스트")
class UserActivityReplaySchedulerTest {

	@Test
	@DisplayName("재처리 배치가 설정된 배치 크기로 runner를 호출한다")
	void replayPendingEvents_invokesRunnerWithBatchSize() {
		UserActivityReplayRunner replayRunner = mock(UserActivityReplayRunner.class);
		when(replayRunner.runPendingReplay(200)).thenReturn(new UserActivityReplayResult(0, 0, 0));
		UserActivityReplayMetricsCollector replayMetricsCollector = new UserActivityReplayMetricsCollector(
			new SimpleMeterRegistry());
		UserActivityReplayScheduler scheduler = new UserActivityReplayScheduler(replayRunner, replayMetricsCollector);
		ReflectionTestUtils.setField(scheduler, "replayBatchSize", 200);

		scheduler.replayPendingEvents();

		verify(replayRunner).runPendingReplay(200);
	}
}
