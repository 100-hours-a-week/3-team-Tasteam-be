package com.tasteam.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.favorite.dto.response.FavoriteCreateResponse;
import com.tasteam.domain.favorite.dto.response.FavoritePageTargetsResponse;
import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.dto.response.RestaurantFavoriteTargetItem;
import com.tasteam.domain.favorite.dto.response.RestaurantFavoriteTargetsResponse;
import com.tasteam.domain.favorite.service.FavoriteService;
import com.tasteam.domain.favorite.type.FavoriteState;
import com.tasteam.domain.favorite.type.FavoriteTargetType;
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.dto.response.MemberPreviewResponse;
import com.tasteam.domain.member.dto.response.MemberSubgroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberSummaryResponse;
import com.tasteam.domain.member.dto.response.ReviewSummaryResponse;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.service.SubgroupFacade;
import com.tasteam.fixture.MemberRequestFixture;

@ControllerWebMvcTest(MemberController.class)
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private SubgroupFacade subgroupFacade;

	@MockitoBean
	private ReviewService reviewService;

	@MockitoBean
	private FavoriteService favoriteService;

	@Nested
	@DisplayName("내 정보 조회")
	class GetMyMemberInfo {

		@Test
		@DisplayName("내 정보를 조회하면 프로필 정보를 반환한다")
		void 내_정보_조회_성공() throws Exception {
			// given
			MemberMeResponse response = new MemberMeResponse(
				new MemberSummaryResponse("테스트유저", "소개", "https://example.com/profile.jpg"),
				MemberPreviewResponse.empty(),
				MemberPreviewResponse.empty());

			given(memberService.getMyProfile(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/members/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.member.nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.member.profileImageUrl").value("https://example.com/profile.jpg"))
				.andExpect(jsonPath("$.data.groupRequests.data").isEmpty())
				.andExpect(jsonPath("$.data.reviews.data").isEmpty());
		}
	}

	@Nested
	@DisplayName("내 그룹 요약 목록 조회")
	class GetMyGroupSummaries {

		@Test
		@DisplayName("내 그룹 목록을 조회하면 그룹과 서브그룹 요약을 반환한다")
		void 내_그룹_요약_조회_성공() throws Exception {
			// given
			List<MemberGroupSummaryResponse> response = List.of(
				new MemberGroupSummaryResponse(1L, "테스트그룹",
					List.of(new MemberSubgroupSummaryResponse(10L, "서브그룹1"))));

			given(memberService.getMyGroupSummaries(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/members/me/groups/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].groupId").value(1))
				.andExpect(jsonPath("$.data[0].groupName").value("테스트그룹"))
				.andExpect(jsonPath("$.data[0].subGroups[0].subGroupId").value(10))
				.andExpect(jsonPath("$.data[0].subGroups[0].subGroupName").value("서브그룹1"));
		}

		@Test
		@DisplayName("가입한 그룹이 없으면 빈 목록을 반환한다")
		void 그룹_없을때_빈_목록() throws Exception {
			// given
			given(memberService.getMyGroupSummaries(any())).willReturn(Collections.emptyList());

			// when & then
			mockMvc.perform(get("/api/v1/members/me/groups/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isEmpty());
		}
	}

	@Nested
	@DisplayName("내 소모임 목록 조회")
	class GetMySubgroups {

		@Test
		@DisplayName("내 소모임 목록을 조회하면 서브그룹 리스트를 반환한다")
		void 내_소모임_목록_조회_성공() throws Exception {
			// given
			SubgroupListItem item = SubgroupListItem.builder()
				.subgroupId(1L)
				.name("서브그룹1")
				.description("설명")
				.memberCount(5)
				.profileImageUrl("https://example.com/img.jpg")
				.createdAt(Instant.now())
				.build();

			SubgroupListResponse response = new SubgroupListResponse(
				List.of(item),
				new SubgroupListResponse.PageInfo("name", null, 20, false));

			given(subgroupFacade.getMySubgroups(eq(1L), any(), any(), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/members/me/groups/{groupId}/subgroups", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data[0].subgroupId").value(1))
				.andExpect(jsonPath("$.data.data[0].name").value("서브그룹1"))
				.andExpect(jsonPath("$.data.page.hasNext").value(false));
		}
	}

	@Nested
	@DisplayName("프로필 수정")
	class UpdateMyProfile {

		@Test
		@DisplayName("프로필을 수정하면 성공 응답을 반환한다")
		void 프로필_수정_성공() throws Exception {
			// given
			willDoNothing().given(memberService).updateMyProfile(any(), any());

			MemberProfileUpdateRequest request = MemberRequestFixture.profileUpdateRequest();

			// when & then
			mockMvc.perform(patch("/api/v1/members/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("내 리뷰 목록 조회")
	class GetMyReviews {

		@Test
		@DisplayName("내 리뷰를 조회하면 커서 페이징 결과를 반환한다")
		void 내_리뷰_조회_성공() throws Exception {
			// given
			CursorPageResponse<ReviewSummaryResponse> response = new CursorPageResponse<>(
				List.of(new ReviewSummaryResponse(1L, "버거킹 판교점", "성남시 분당구 대왕판교로", "최고의 맛집. 추천합니다.")),
				new CursorPageResponse.Pagination(null, false, 1));

			given(reviewService.getMemberReviews(anyLong(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/members/me/reviews"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].restaurantName").value("버거킹 판교점"))
				.andExpect(jsonPath("$.data.items[0].restaurantAddress").value("성남시 분당구 대왕판교로"))
				.andExpect(jsonPath("$.data.items[0].reviewContent").value("최고의 맛집. 추천합니다."))
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}

		@Test
		@DisplayName("리뷰가 없으면 빈 목록을 반환한다")
		void 리뷰_없을때_빈_목록() throws Exception {
			// given
			given(reviewService.getMemberReviews(anyLong(), any())).willReturn(CursorPageResponse.empty());

			// when & then
			mockMvc.perform(get("/api/v1/members/me/reviews"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}
	}

	@Nested
	@DisplayName("내 찜 음식점 목록 조회")
	class GetMyFavoriteRestaurants {

		@Test
		@DisplayName("내 찜 음식점을 조회하면 커서 페이징 결과를 반환한다")
		void 내_찜_음식점_조회_성공() throws Exception {
			// given
			Instant now = Instant.now();
			CursorPageResponse<FavoriteRestaurantItem> response = new CursorPageResponse<>(
				List.of(new FavoriteRestaurantItem(101L, "국밥집", "https://cdn.example.com/restaurants/101.jpg",
					List.of("한식", "국밥"), "서울시 강남구", now)),
				new CursorPageResponse.Pagination(null, false, 1));

			given(favoriteService.getMyFavoriteRestaurants(any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/members/me/favorites/restaurants"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].restaurantId").value(101))
				.andExpect(jsonPath("$.data.items[0].name").value("국밥집"))
				.andExpect(
					jsonPath("$.data.items[0].thumbnailUrl").value("https://cdn.example.com/restaurants/101.jpg"))
				.andExpect(jsonPath("$.data.items[0].createdAt").exists())
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}

		@Test
		@DisplayName("찜한 음식점이 없으면 빈 목록을 반환한다")
		void 찜_없을때_빈_목록() throws Exception {
			// given
			given(favoriteService.getMyFavoriteRestaurants(any(), any())).willReturn(CursorPageResponse.empty());

			// when & then
			mockMvc.perform(get("/api/v1/members/me/favorites/restaurants"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}
	}

	@Nested
	@DisplayName("회원 탈퇴")
	class Withdraw {

		@Test
		@DisplayName("회원 탈퇴하면 성공 응답을 반환한다")
		void 회원_탈퇴_성공() throws Exception {
			// given
			willDoNothing().given(memberService).withdraw(any());

			// when & then
			mockMvc.perform(delete("/api/v1/members/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("내 찜 추가/삭제")
	class MyFavoriteCommands {

		@Test
		@DisplayName("내 찜 등록에 성공하면 생성 응답을 반환한다")
		void 내_찜_등록_성공() throws Exception {
			Instant now = Instant.now();
			given(favoriteService.createMyFavorite(anyLong(), anyLong()))
				.willReturn(new FavoriteCreateResponse(10L, 101L, now));

			mockMvc.perform(post("/api/v1/members/me/favorites/restaurants")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"restaurantId":101}
					"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.restaurantId").value(101));
		}

		@Test
		@DisplayName("내 찜 삭제에 성공하면 성공 응답을 반환한다")
		void 내_찜_삭제_성공() throws Exception {
			willDoNothing().given(favoriteService).deleteMyFavorite(anyLong(), anyLong());

			mockMvc.perform(delete("/api/v1/members/me/favorites/restaurants/{restaurantId}", 101L))
				.andExpect(status().isNoContent());
		}
	}

	@Nested
	@DisplayName("찜 타겟 조회")
	class FavoriteTargets {

		@Test
		@DisplayName("찜 타겟 목록을 조회하면 내 찜과 소모임 타겟을 반환한다")
		void 찜_타겟_조회_성공() throws Exception {
			FavoritePageTargetsResponse response = new FavoritePageTargetsResponse(
				new FavoritePageTargetsResponse.MyFavoriteTarget(3L),
				List.of(new FavoritePageTargetsResponse.SubgroupFavoriteTarget(22L, "점심팟", 5L)));
			given(favoriteService.getFavoriteTargets(anyLong())).willReturn(response);

			mockMvc.perform(get("/api/v1/members/me/favorite-targets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.myFavorite.favoriteCount").value(3))
				.andExpect(jsonPath("$.data.subgroupFavorites[0].subgroupId").value(22));
		}

		@Test
		@DisplayName("음식점 맥락 찜 타겟을 조회하면 상태 정보를 반환한다")
		void 음식점_맥락_찜_타겟_조회_성공() throws Exception {
			RestaurantFavoriteTargetsResponse response = new RestaurantFavoriteTargetsResponse(List.of(
				new RestaurantFavoriteTargetItem(FavoriteTargetType.ME, null, "내 찜", FavoriteState.FAVORITED),
				new RestaurantFavoriteTargetItem(FavoriteTargetType.SUBGROUP, 22L, "점심팟",
					FavoriteState.NOT_FAVORITED)));
			given(favoriteService.getFavoriteTargets(anyLong(), anyLong())).willReturn(response);

			mockMvc.perform(get("/api/v1/members/me/restaurants/{restaurantId}/favorite-targets", 101L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.targets[0].favoriteState").value("FAVORITED"))
				.andExpect(jsonPath("$.data.targets[1].favoriteState").value("NOT_FAVORITED"));
		}
	}
}
