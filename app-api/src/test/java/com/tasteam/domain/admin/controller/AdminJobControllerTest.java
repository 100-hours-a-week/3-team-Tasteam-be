package com.tasteam.domain.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.batch.image.optimization.service.ImageOptimizationService;
import com.tasteam.config.BaseAdminControllerWebMvcTest;

@DisplayName("[유닛](Admin) AdminJobController 단위 테스트")
class AdminJobControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("최적화 대상 탐색")
	class DiscoverOptimizationTargets {

		@Test
		@DisplayName("대상 탐색이 성공하면 요청 처리 통계를 반환한다")
		void 최적화_대상탐색_성공() throws Exception {
			// given
			given(imageOptimizationService.discoverOptimizationTargets()).willReturn(2);

			// when & then
			mockMvc.perform(post("/api/v1/admin/jobs/image-optimization/discover"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.jobName").value("image-optimization-discover"))
				.andExpect(jsonPath("$.data.successCount").value(2));
		}

		@Test
		@DisplayName("대상 탐색 중 예외가 발생하면 500으로 실패한다")
		void 최적화_대상탐색_실패() throws Exception {
			// given
			given(imageOptimizationService.discoverOptimizationTargets())
				.willThrow(new RuntimeException("discover error"));

			// when & then
			mockMvc.perform(post("/api/v1/admin/jobs/image-optimization/discover"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("대기 중인 최적화 작업 조회")
	class GetPendingOptimizationJobs {

		@Test
		@DisplayName("대기 중인 작업 목록을 조회하면 빈 목록을 반환한다")
		void 대기_작업_조회_성공() throws Exception {
			// given
			given(imageOptimizationService.findPendingJobs(100)).willReturn(List.of());

			// when & then
			mockMvc.perform(get("/api/v1/admin/jobs/image-optimization/pending").param("limit", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(0));
		}

		@Test
		@DisplayName("조회 중 오류가 발생하면 500으로 실패한다")
		void 대기_작업_조회_실패() throws Exception {
			// given
			given(imageOptimizationService.findPendingJobs(100)).willThrow(new RuntimeException("db error"));

			// when & then
			mockMvc.perform(get("/api/v1/admin/jobs/image-optimization/pending").param("limit", "100"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("최적화 배치 실행")
	class RunOptimization {

		@Test
		@DisplayName("배치 실행이 성공하면 결과 집계를 반환한다")
		void 최적화_배치_성공() throws Exception {
			// given
			var result = new ImageOptimizationService.OptimizationResult(3, 1, 0);
			given(imageOptimizationService.processOptimizationBatch(100)).willReturn(result);

			// when & then
			mockMvc.perform(post("/api/v1/admin/jobs/image-optimization").param("batchSize", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.jobName").value("image-optimization"))
				.andExpect(jsonPath("$.data.successCount").value(3))
				.andExpect(jsonPath("$.data.failedCount").value(1));
		}

		@Test
		@DisplayName("배치 실행 중 오류가 발생하면 500으로 실패한다")
		void 최적화_배치_실패() throws Exception {
			// given
			given(imageOptimizationService.processOptimizationBatch(100)).willThrow(new RuntimeException("batch fail"));

			// when & then
			mockMvc.perform(post("/api/v1/admin/jobs/image-optimization").param("batchSize", "100"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("최적화 작업 삭제")
	class DeleteOptimizationJobs {

		@Test
		@DisplayName("최적화 작업 삭제 시 본문 없이 204를 반환한다")
		void 최적화_작업_삭제_성공() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/admin/jobs/image-optimization"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("삭제 로직 실패 시 500으로 실패한다")
		void 최적화_작업_삭제_실패() throws Exception {
			// given
			org.mockito.Mockito.doThrow(new RuntimeException("delete failed")).when(imageOptimizationService)
				.deleteAllJobs();

			// when & then
			mockMvc.perform(delete("/api/v1/admin/jobs/image-optimization"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("이미지 정리 대기 목록 조회")
	class GetCleanupPendingImages {

		@Test
		@DisplayName("정리 대기 이미지 목록을 조회하면 목록을 반환한다")
		void 정리_이미지_조회_성공() throws Exception {
			// given
			given(fileService.findCleanupPendingImages()).willReturn(List.of());

			// when & then
			mockMvc.perform(get("/api/v1/admin/jobs/image-cleanup/pending"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(0));
		}

		@Test
		@DisplayName("정리 대상 조회 실패 시 500으로 실패한다")
		void 정리_이미지_조회_실패() throws Exception {
			// given
			given(fileService.findCleanupPendingImages()).willThrow(new RuntimeException("cleanup query fail"));

			// when & then
			mockMvc.perform(get("/api/v1/admin/jobs/image-cleanup/pending"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("이미지 정리 실행")
	class RunImageCleanup {

		@Test
		@DisplayName("이미지 정리가 성공하면 처리 개수를 반환한다")
		void 이미지_정리_성공() throws Exception {
			// given
			given(fileService.cleanupPendingDeletedImages()).willReturn(5);

			// when & then
			mockMvc.perform(post("/api/v1/admin/jobs/image-cleanup"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.jobName").value("image-cleanup"))
				.andExpect(jsonPath("$.data.successCount").value(5));
		}

		@Test
		@DisplayName("이미지 정리 실패 시 500으로 실패한다")
		void 이미지_정리_실패() throws Exception {
			// given
			given(fileService.cleanupPendingDeletedImages()).willThrow(new RuntimeException("cleanup fail"));

			// when & then
			mockMvc.perform(post("/api/v1/admin/jobs/image-cleanup"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}
}
