package com.tasteam.domain.analytics.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](UserActivity) UserActivityReplayRunner 단위 테스트")
class UserActivityReplayRunnerTest {

	@Test
	@DisplayName("재처리 배치 호출과 결과 반환을 정상 처리한다")
	void runPendingReplay_recordsReplayMetrics() {
		UserActivityReplayService replayService = mock(UserActivityReplayService.class);
		when(replayService.replayPending(200)).thenReturn(new UserActivityReplayResult(4, 3, 1));
		UserActivityReplayRunner runner = new UserActivityReplayRunner(replayService);

		UserActivityReplayResult result = runner.runPendingReplay(200);

		verify(replayService).replayPending(200);
		assertThat(result).isNotNull();
		assertThat(result.processedCount()).isEqualTo(4);
		assertThat(result.successCount()).isEqualTo(3);
		assertThat(result.failedCount()).isEqualTo(1);
	}
}
