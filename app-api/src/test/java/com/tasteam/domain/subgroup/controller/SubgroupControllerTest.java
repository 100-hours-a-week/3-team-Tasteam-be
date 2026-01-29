package com.tasteam.domain.subgroup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.service.SubgroupService;

@ControllerWebMvcTest(SubgroupController.class)
class SubgroupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewService reviewService;

	@MockitoBean
	private SubgroupService subgroupService;

	@Nested
	@DisplayName("서브그룹 리뷰 목록 조회")
	class GetSubgroupReviews {

		@Test
		@DisplayName("서브그룹 리뷰를 조회하면 커서 페이징 결과를 반환한다")
		void 서브그룹_리뷰_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<ReviewResponse> response = new CursorPageResponse<>(
				List.of(new ReviewResponse(1L,
					new ReviewResponse.AuthorResponse("테스트유저"),
					"맛있어요", true, List.of("친절"),
					new ReviewResponse.ReviewImageResponse(1L, "https://example.com/review.jpg"),
					Instant.now())),
				new CursorPageResponse.Pagination(null, false, 20));

			given(reviewService.getSubgroupReviews(eq(1L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/subgroups/{subgroupId}/reviews", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].author.nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}
	}

	@Nested
	@DisplayName("서브그룹 멤버 목록 조회")
	class GetSubgroupMembers {

		@Test
		@DisplayName("서브그룹 멤버를 조회하면 커서 페이징 결과를 반환한다")
		void 서브그룹_멤버_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<SubgroupMemberListItem> response = new CursorPageResponse<>(
				List.of(new SubgroupMemberListItem(1L, 100L, "테스트유저",
					"https://example.com/profile.jpg", Instant.now())),
				new CursorPageResponse.Pagination(null, false, 20));

			given(subgroupService.getSubgroupMembers(eq(1L), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/subgroups/{subgroupId}/members", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].memberId").value(100))
				.andExpect(jsonPath("$.data.items[0].nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}
	}
}
