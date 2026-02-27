package com.tasteam.domain.subgroup.controller;

import static org.mockito.ArgumentMatchers.*;
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
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.service.SubgroupFacade;

@ControllerWebMvcTest(SubgroupController.class)
class SubgroupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewService reviewService;

	@MockitoBean
	private SubgroupFacade subgroupFacade;

	@Nested
	@DisplayName("서브그룹 리뷰 목록 조회")
	class GetSubgroupReviews {

		@Test
		@DisplayName("서브그룹 리뷰를 조회하면 커서 페이징 결과를 반환한다")
		void 서브그룹_리뷰_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<ReviewResponse> response = new CursorPageResponse<>(
				List.of(new ReviewResponse(1L, 2L, 3L, "테스트그룹", "테스트하위그룹",
					new ReviewResponse.AuthorResponse("테스트유저", "https://example.com/profile.jpg"),
					"맛있어요", true, List.of("친절"),
					List.of(new ReviewResponse.ReviewImageResponse(1L, "https://example.com/review.jpg")),
					Instant.now(), 10L, "테스트음식점", null, null, null, "서울시 강남구 테스트로 123")),
				new CursorPageResponse.Pagination(null, false, 20));

			given(reviewService.getSubgroupReviews(eq(1L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/subgroups/{subgroupId}/reviews", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].author.nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.items[0].author.profileImageUrl").value("https://example.com/profile.jpg"))
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

			given(subgroupFacade.getSubgroupMembers(eq(1L), anyLong(), any(), any())).willReturn(response);

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

	@Nested
	@DisplayName("서브그룹 상세 조회")
	class GetSubgroup {

		@Test
		@DisplayName("서브그룹 ID로 상세 정보를 조회하면 서브그룹 정보를 반환한다")
		void 서브그룹_상세_조회_성공() throws Exception {
			// given
			SubgroupDetailResponse response = new SubgroupDetailResponse(
				new SubgroupDetailResponse.SubgroupDetail(
					1L, 10L, "서브그룹1", "설명", 5,
					"https://example.com/img.jpg", Instant.now()));

			given(subgroupFacade.getSubgroup(eq(10L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/subgroups/{subgroupId}", 10L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data.subgroupId").value(10))
				.andExpect(jsonPath("$.data.data.name").value("서브그룹1"))
				.andExpect(jsonPath("$.data.data.memberCount").value(5));
		}
	}

	@Nested
	@DisplayName("서브그룹 탈퇴")
	class WithdrawSubgroup {

		@Test
		@DisplayName("서브그룹에서 탈퇴하면 204를 반환한다")
		void 서브그룹_탈퇴_성공() throws Exception {
			// given
			willDoNothing().given(subgroupFacade).withdrawSubgroup(eq(10L), any());

			// when & then
			mockMvc.perform(delete("/api/v1/subgroups/{subgroupId}/members/me", 10L))
				.andExpect(status().isNoContent());
		}
	}
}
