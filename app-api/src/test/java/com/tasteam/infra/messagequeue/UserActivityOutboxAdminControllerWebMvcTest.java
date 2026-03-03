package com.tasteam.infra.messagequeue;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.analytics.resilience.UserActivityReplayResult;
import com.tasteam.domain.analytics.resilience.UserActivityReplayService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxSummary;

@TestPropertySource(properties = "tasteam.message-queue.enabled=true")
@ControllerWebMvcTest(UserActivityOutboxAdminController.class)
@DisplayName("[유닛](MQ) UserActivityOutboxAdminController 단위 테스트")
class UserActivityOutboxAdminControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserActivitySourceOutboxService outboxService;

	@MockitoBean
	private UserActivityReplayService replayService;

	@Nested
	@DisplayName("아웃박스 요약 조회")
	class GetSummary {

		@Test
		@DisplayName("요약 조회에 성공하면 집계 데이터를 반환한다")
		void 요약_조회_성공() throws Exception {
			// given
			given(outboxService.summarize())
				.willReturn(new UserActivitySourceOutboxSummary(3, 2, 10, 4));

			// when & then
			mockMvc.perform(get("/api/v1/admin/user-activity/outbox/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.pendingCount").value(3))
				.andExpect(jsonPath("$.data.failedCount").value(2))
				.andExpect(jsonPath("$.data.publishedCount").value(10))
				.andExpect(jsonPath("$.data.maxRetryCount").value(4));
		}

		@Test
		@DisplayName("요약 조회에 실패하면 500으로 실패한다")
		void 요약_조회_실패() throws Exception {
			// given
			willThrow(new RuntimeException("summary fail")).given(outboxService).summarize();

			// when & then
			mockMvc.perform(get("/api/v1/admin/user-activity/outbox/summary"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("아웃박스 재처리")
	class Replay {

		@Test
		@DisplayName("요청 제한값을 초과하면 500으로 클램프해서 재처리한다")
		void 재처리_클램프_적용_성공() throws Exception {
			// given
			var result = new UserActivityReplayResult(5, 4, 1);
			given(replayService.replayPending(500)).willReturn(result);

			// when & then
			mockMvc.perform(post("/api/v1/admin/user-activity/outbox/replay")
				.contentType(APPLICATION_JSON)
				.param("limit", "999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.processedCount").value(5))
				.andExpect(jsonPath("$.data.successCount").value(4))
				.andExpect(jsonPath("$.data.failedCount").value(1));

			verify(replayService).replayPending(500);
		}

		@Test
		@DisplayName("limit가 숫자가 아니면 400으로 실패한다")
		void 재처리_파라미터_타입_실패() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/admin/user-activity/outbox/replay").param("limit", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}

		@Test
		@DisplayName("재처리 실행 중 실패하면 500으로 실패한다")
		void 재처리_실패() throws Exception {
			// given
			willThrow(new RuntimeException("replay fail")).given(replayService).replayPending(anyInt());

			// when & then
			mockMvc.perform(post("/api/v1/admin/user-activity/outbox/replay"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}
}
