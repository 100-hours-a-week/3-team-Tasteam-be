package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.batch.dummy.DummySeedJobTracker;
import com.tasteam.batch.dummy.service.DummyDataSeedService;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.domain.admin.dto.response.AdminDataCountResponse;
import com.tasteam.domain.admin.dto.response.DummySeedStatusResponse;

@ControllerWebMvcTest(AdminDummyController.class)
@DisplayName("[유닛](Admin) AdminDummyController 단위 테스트")
class AdminDummyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private DummyDataSeedService dummyDataSeedService;

	@MockitoBean
	private DummySeedJobTracker jobTracker;

	@Nested
	@DisplayName("더미 데이터 생성")
	class Seed {

		@Test
		@DisplayName("시딩이 진행 중이지 않으면 202를 반환한다")
		void 더미_시드_성공() throws Exception {
			// given
			var request = new AdminDummySeedRequest(1, 2, 0, 0, 0, 0, 0, 0, 0);
			given(jobTracker.isRunning()).willReturn(false);
			given(dummyDataSeedService.seedAsync(any())).willReturn(CompletableFuture.completedFuture(null));

			// when & then
			mockMvc.perform(post("/api/v1/admin/dummy/seed")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isAccepted());
		}

		@Test
		@DisplayName("이미 시딩이 진행 중이면 409를 반환한다")
		void 더미_시드_중복_실패() throws Exception {
			// given
			var request = new AdminDummySeedRequest(1, 2, 0, 0, 0, 0, 0, 0, 0);
			given(jobTracker.isRunning()).willReturn(true);

			// when & then
			mockMvc.perform(post("/api/v1/admin/dummy/seed")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SEED_ALREADY_RUNNING"));
		}

		@Test
		@DisplayName("음수 값이 들어오면 400으로 실패한다")
		void 더미_시드_음수값_실패() throws Exception {
			// given
			var request = new AdminDummySeedRequest(-1, 0, 0, 0, 0, 0, 0, 0, 0);

			// when & then
			mockMvc.perform(post("/api/v1/admin/dummy/seed")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("시딩 진행 상태 조회")
	class SeedStatus {

		@Test
		@DisplayName("현재 시딩 상태를 정상 조회한다")
		void 시딩_상태_조회_성공() throws Exception {
			// given
			var snapshot = new DummySeedStatusResponse("RUNNING", "member insert", 1, 7, null, null, 1500L);
			given(jobTracker.getSnapshot()).willReturn(snapshot);

			// when & then
			mockMvc.perform(get("/api/v1/admin/dummy/seed/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("RUNNING"))
				.andExpect(jsonPath("$.data.completedSteps").value(1))
				.andExpect(jsonPath("$.data.totalSteps").value(7));
		}
	}

	@Nested
	@DisplayName("더미 데이터 카운트")
	class Count {

		@Test
		@DisplayName("현재 더미 데이터 집계를 정상 조회한다")
		void 더미_카운트_성공() throws Exception {
			// given
			given(dummyDataSeedService.count()).willReturn(new AdminDataCountResponse(1, 2, 3, 4, 5, 6, 7, 8));

			// when & then
			mockMvc.perform(get("/api/v1/admin/dummy/count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberCount").value(1))
				.andExpect(jsonPath("$.data.restaurantCount").value(2))
				.andExpect(jsonPath("$.data.groupCount").value(3))
				.andExpect(jsonPath("$.data.subgroupCount").value(4));
		}
	}

	@Nested
	@DisplayName("더미 데이터 삭제")
	class DeleteDummy {

		@Test
		@DisplayName("더미 데이터 삭제 시 본문 없이 204를 반환한다")
		void 더미_삭제_성공() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/admin/dummy"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("삭제 처리 중 예외가 발생하면 500으로 실패한다")
		void 더미_삭제_실패() throws Exception {
			// given
			willThrow(new RuntimeException("seed cleanup failed"))
				.given(dummyDataSeedService)
				.deleteDummyData();

			// when & then
			mockMvc.perform(delete("/api/v1/admin/dummy"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}
}
