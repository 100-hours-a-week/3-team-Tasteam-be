package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.tasteam.config.BaseAdminControllerWebMvcTest;
import com.tasteam.domain.admin.dto.response.AdminReviewListItem;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.ReviewErrorCode;

@DisplayName("[유닛](Admin) AdminReviewController 단위 테스트")
class AdminReviewControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("리뷰 목록 조회")
	class GetReviews {

		@Test
		@DisplayName("리뷰 목록을 조회하면 페이지 결과를 반환한다")
		void 리뷰_목록_조회_성공() throws Exception {
			// given
			var item = new AdminReviewListItem(
				1L,
				2L,
				"테스트 식당",
				10L,
				"작성자",
				"좋아요",
				true,
				Instant.parse("2026-02-01T09:00:00Z"));
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			Page<AdminReviewListItem> reviews = new PageImpl<>(List.of(item), pageable, 1);
			given(adminReviewService.getReviews(any(), any(Pageable.class))).willReturn(reviews);

			// when & then
			mockMvc.perform(get("/api/v1/admin/reviews")
				.param("restaurantId", "2")
				.param("page", "0")
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content[0].id").value(1))
				.andExpect(jsonPath("$.data.content[0].restaurantName").value("테스트 식당"));
		}

		@Test
		@DisplayName("레스토랑 ID 타입이 잘못되면 400으로 실패한다")
		void 리뷰_목록_레스토랑_타입_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/reviews").param("restaurantId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("리뷰 삭제")
	class DeleteReview {

		@Test
		@DisplayName("리뷰가 존재하면 204를 반환한다")
		void 리뷰_삭제_성공() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/admin/reviews/1"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("리뷰가 없으면 404로 실패한다")
		void 리뷰_삭제_미존재_실패() throws Exception {
			// given
			doThrow(new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND))
				.when(adminReviewService)
				.deleteReview(999L);

			// when & then
			mockMvc.perform(delete("/api/v1/admin/reviews/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
		}
	}
}
