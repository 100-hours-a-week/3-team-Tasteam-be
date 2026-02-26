package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.resilience.UserActivityReplayResult;
import com.tasteam.domain.analytics.resilience.UserActivityReplayService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxSummary;
import com.tasteam.global.dto.api.SuccessResponse;

@UnitTest
@DisplayName("사용자 이벤트 outbox 관리자 컨트롤러")
class UserActivityOutboxAdminControllerTest {

	@Test
	@DisplayName("요약 조회를 호출하면 outbox 상태 집계를 반환한다")
	void getSummary_returnsOutboxSummary() {
		// given
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityReplayService replayService = mock(UserActivityReplayService.class);
		UserActivityOutboxAdminController controller = new UserActivityOutboxAdminController(outboxService,
			replayService);
		when(outboxService.summarize()).thenReturn(new UserActivitySourceOutboxSummary(3, 2, 10, 4));

		// when
		SuccessResponse<UserActivityOutboxSummaryResponse> response = controller.getSummary();

		// then
		assertThat(response.getData()).isNotNull();
		assertThat(response.getData().pendingCount()).isEqualTo(3);
		assertThat(response.getData().failedCount()).isEqualTo(2);
		assertThat(response.getData().publishedCount()).isEqualTo(10);
		assertThat(response.getData().maxRetryCount()).isEqualTo(4);
	}

	@Test
	@DisplayName("재처리 실행을 호출하면 처리 결과를 반환한다")
	void replay_returnsReplayResult() {
		// given
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityReplayService replayService = mock(UserActivityReplayService.class);
		UserActivityOutboxAdminController controller = new UserActivityOutboxAdminController(outboxService,
			replayService);
		when(replayService.replayPending(500)).thenReturn(new UserActivityReplayResult(5, 4, 1));

		// when
		SuccessResponse<UserActivityReplayResponse> response = controller.replay(1000);

		// then
		verify(replayService).replayPending(500);
		assertThat(response.getData()).isNotNull();
		assertThat(response.getData().processedCount()).isEqualTo(5);
		assertThat(response.getData().successCount()).isEqualTo(4);
		assertThat(response.getData().failedCount()).isEqualTo(1);
	}
}
