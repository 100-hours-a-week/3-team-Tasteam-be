package com.tasteam.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.review.dto.response.ReviewDetailResponse;
import com.tasteam.domain.review.dto.response.ReviewKeywordItemResponse;
import com.tasteam.domain.review.entity.KeywordType;
import com.tasteam.domain.review.service.ReviewService;

@ControllerWebMvcTest(ReviewController.class)
class ReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewService reviewService;

	@Nested
	@DisplayName("리뷰 키워드 목록 조회")
	class GetReviewKeywords {

		@Test
		@DisplayName("키워드 목록을 조회하면 전체 키워드를 반환한다")
		void 키워드_목록_조회_성공() throws Exception {
			// given
			List<ReviewKeywordItemResponse> response = List.of(
				new ReviewKeywordItemResponse(1L, KeywordType.VISIT_PURPOSE, "데이트"),
				new ReviewKeywordItemResponse(2L, KeywordType.COMPANION_TYPE, "연인"),
				new ReviewKeywordItemResponse(3L, KeywordType.POSITIVE_ASPECT, "친절"));

			given(reviewService.getReviewKeywords(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/reviews/keywords"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].type").value("VISIT_PURPOSE"))
				.andExpect(jsonPath("$.data[0].name").value("데이트"))
				.andExpect(jsonPath("$.data[2].type").value("POSITIVE_ASPECT"));
		}

		@Test
		@DisplayName("키워드 타입으로 필터링하여 조회할 수 있다")
		void 키워드_타입_필터_조회() throws Exception {
			// given
			List<ReviewKeywordItemResponse> response = List.of(
				new ReviewKeywordItemResponse(1L, KeywordType.VISIT_PURPOSE, "데이트"),
				new ReviewKeywordItemResponse(2L, KeywordType.VISIT_PURPOSE, "회식"));

			given(reviewService.getReviewKeywords(eq(KeywordType.VISIT_PURPOSE))).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/reviews/keywords")
				.param("type", "VISIT_PURPOSE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(2));
		}
	}

	@Nested
	@DisplayName("리뷰 상세 조회")
	class GetReview {

		@Test
		@DisplayName("리뷰 ID로 상세 정보를 조회하면 리뷰 정보를 반환한다")
		void 리뷰_상세_조회_성공() throws Exception {
			// given
			ReviewDetailResponse response = new ReviewDetailResponse(
				1L,
				new ReviewDetailResponse.RestaurantResponse(10L, "맛집식당"),
				new ReviewDetailResponse.AuthorResponse(100L, "테스트유저", "https://example.com/profile.jpg"),
				"맛있어요",
				true,
				List.of("친절", "깨끗"),
				List.of(new ReviewDetailResponse.ReviewImageResponse(1L, "https://example.com/review.jpg")),
				Instant.now(),
				Instant.now());

			given(reviewService.getReviewDetail(1L)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/reviews/{reviewId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.restaurant.id").value(10))
				.andExpect(jsonPath("$.data.restaurant.name").value("맛집식당"))
				.andExpect(jsonPath("$.data.author.nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.author.profileImageUrl").value("https://example.com/profile.jpg"))
				.andExpect(jsonPath("$.data.content").value("맛있어요"))
				.andExpect(jsonPath("$.data.isRecommended").value(true))
				.andExpect(jsonPath("$.data.keywords[0]").value("친절"))
				.andExpect(jsonPath("$.data.images[0].url").value("https://example.com/review.jpg"));
		}
	}

	@Nested
	@DisplayName("리뷰 삭제")
	class DeleteReview {

		@Test
		@DisplayName("리뷰를 삭제하면 204를 반환한다")
		void 리뷰_삭제_성공() throws Exception {
			// given
			willDoNothing().given(reviewService).deleteReview(1L, 1L);

			// when & then
			mockMvc.perform(delete("/api/v1/reviews/{reviewId}", 1L))
				.andExpect(status().isNoContent());
		}
	}
}
