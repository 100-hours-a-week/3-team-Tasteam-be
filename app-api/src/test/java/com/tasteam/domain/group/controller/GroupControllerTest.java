package com.tasteam.domain.group.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListItem;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.service.GroupService;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.service.SubgroupService;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.fixture.GroupRequestFixture;
import com.tasteam.fixture.RestaurantRequestFixture;

@ControllerWebMvcTest(GroupController.class)
class GroupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private GroupService groupService;

	@MockitoBean
	private RestaurantService restaurantService;

	@MockitoBean
	private ReviewService reviewService;

	@MockitoBean
	private SubgroupService subgroupService;

	@Nested
	@DisplayName("그룹 생성")
	class CreateGroup {

		@Test
		@DisplayName("그룹을 생성하면 201과 생성된 ID를 반환한다")
		void 그룹_생성_성공() throws Exception {
			// given
			GroupCreateResponse response = new GroupCreateResponse(1L, "ACTIVE", Instant.now());
			given(groupService.createGroup(any())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/groups")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(GroupRequestFixture.createGroupRequest())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));
		}
	}

	@Nested
	@DisplayName("그룹 상세 조회")
	class GetGroup {

		@Test
		@DisplayName("그룹 ID로 상세 정보를 조회하면 그룹 정보를 반환한다")
		void 그룹_상세_조회_성공() throws Exception {
			// given
			GroupGetResponse response = new GroupGetResponse(
				new GroupGetResponse.GroupData(
					1L, "테스트그룹", "https://example.com/logo.jpg",
					"서울시 강남구", null, "test.com", 3L, "ACTIVE",
					Instant.now(), Instant.now()));

			given(groupService.getGroup(1L)).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/groups/{groupId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data.groupId").value(1))
				.andExpect(jsonPath("$.data.data.name").value("테스트그룹"))
				.andExpect(jsonPath("$.data.data.status").value("ACTIVE"));
		}
	}

	@Nested
	@DisplayName("그룹 수정")
	class UpdateGroup {

		@Test
		@DisplayName("그룹 정보를 수정하면 성공 응답을 반환한다")
		void 그룹_수정_성공() throws Exception {
			// given
			willDoNothing().given(groupService).updateGroup(eq(1L), any());

			// when & then
			mockMvc.perform(patch("/api/v1/groups/{groupId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(GroupRequestFixture.createUpdateRequest())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 삭제")
	class DeleteGroup {

		@Test
		@DisplayName("그룹을 삭제하면 성공 응답을 반환한다")
		void 그룹_삭제_성공() throws Exception {
			// given
			willDoNothing().given(groupService).deleteGroup(1L);

			// when & then
			mockMvc.perform(delete("/api/v1/groups/{groupId}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 탈퇴")
	class WithdrawGroup {

		@Test
		@DisplayName("그룹에서 탈퇴하면 성공 응답을 반환한다")
		void 그룹_탈퇴_성공() throws Exception {
			// given
			willDoNothing().given(groupService).withdrawGroup(eq(1L), any());

			// when & then
			mockMvc.perform(delete("/api/v1/groups/{groupId}/members/me", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 서브그룹 목록 조회")
	class GetGroupSubgroups {

		@Test
		@DisplayName("그룹의 서브그룹 목록을 조회하면 서브그룹 리스트를 반환한다")
		void 그룹_서브그룹_목록_조회_성공() throws Exception {
			// given
			SubgroupListItem item = SubgroupListItem.builder()
				.subgroupId(1L)
				.name("서브그룹1")
				.memberCount(10)
				.createdAt(Instant.now())
				.build();

			CursorPageResponse<SubgroupListItem> response = new CursorPageResponse<>(
				List.of(item),
				new CursorPageResponse.Pagination(null, false, 1));

			given(subgroupService.getGroupSubgroups(eq(1L), any(), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/groups/{groupId}/subgroups", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].subgroupId").value(1))
				.andExpect(jsonPath("$.data.items[0].name").value("서브그룹1"));
		}
	}

	@Nested
	@DisplayName("서브그룹 생성")
	class CreateSubgroup {

		@Test
		@DisplayName("서브그룹을 생성하면 201과 생성 정보를 반환한다")
		void 서브그룹_생성_성공() throws Exception {
			// given
			SubgroupCreateResponse response = new SubgroupCreateResponse(
				new SubgroupCreateResponse.SubgroupCreateData(10L, Instant.now()));

			given(subgroupService.createSubgroup(eq(1L), any(), any())).willReturn(response);

			SubgroupCreateRequest request = new SubgroupCreateRequest(
				"서브그룹1", "설명", null, SubgroupJoinType.OPEN, null);

			// when & then
			mockMvc.perform(post("/api/v1/groups/{groupId}/subgroups", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data.id").value(10));
		}
	}

	@Nested
	@DisplayName("서브그룹 가입")
	class JoinSubgroup {

		@Test
		@DisplayName("서브그룹에 가입하면 가입 정보를 반환한다")
		void 서브그룹_가입_성공() throws Exception {
			// given
			SubgroupJoinResponse response = new SubgroupJoinResponse(
				new SubgroupJoinResponse.JoinData(10L, Instant.now()));

			given(subgroupService.joinSubgroup(eq(1L), eq(10L), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/groups/{groupId}/subgroups/{subgroupId}/members", 1L, 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data.subgroupId").value(10));
		}
	}

	@Nested
	@DisplayName("서브그룹 수정")
	class UpdateSubgroup {

		@Test
		@DisplayName("서브그룹 정보를 수정하면 성공 응답을 반환한다")
		void 서브그룹_수정_성공() throws Exception {
			// given
			willDoNothing().given(subgroupService).updateSubgroup(eq(1L), eq(10L), any(), any());

			SubgroupUpdateRequest request = new SubgroupUpdateRequest(null, null, null);

			// when & then
			mockMvc.perform(patch("/api/v1/groups/{groupId}/subgroups/{subgroupId}", 1L, 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 이메일 인증 코드 발송")
	class SendEmailVerification {

		@Test
		@DisplayName("이메일 인증 코드를 발송하면 발송 정보를 반환한다")
		void 이메일_인증_코드_발송_성공() throws Exception {
			// given
			GroupEmailVerificationResponse response = new GroupEmailVerificationResponse(
				1L, Instant.now(), Instant.now().plusSeconds(600));

			given(groupService.sendGroupEmailVerification(eq(1L), any())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/groups/{groupId}/email-verifications", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(GroupRequestFixture.createEmailVerificationRequest())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1));
		}
	}

	@Nested
	@DisplayName("그룹 이메일 인증")
	class AuthenticateByEmail {

		@Test
		@DisplayName("이메일 인증에 성공하면 201과 인증 결과를 반환한다")
		void 이메일_인증_성공() throws Exception {
			// given
			GroupEmailAuthenticationResponse response = new GroupEmailAuthenticationResponse(true, Instant.now());

			given(groupService.authenticateGroupByEmail(eq(1L), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/groups/{groupId}/email-authentications", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(GroupRequestFixture.createEmailAuthenticationRequest())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.verified").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 비밀번호 인증")
	class AuthenticateByPassword {

		@Test
		@DisplayName("비밀번호 인증에 성공하면 201과 인증 결과를 반환한다")
		void 비밀번호_인증_성공() throws Exception {
			// given
			GroupPasswordAuthenticationResponse response = new GroupPasswordAuthenticationResponse(true, Instant.now());

			given(groupService.authenticateGroupByPassword(eq(1L), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(post("/api/v1/groups/{groupId}/password-authentications", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(GroupRequestFixture.createPasswordAuthenticationRequest())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.verified").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 멤버 목록 조회")
	class GetGroupMembers {

		@Test
		@DisplayName("그룹 멤버 목록을 조회하면 커서 페이징 결과를 반환한다")
		void 그룹_멤버_목록_조회_성공() throws Exception {
			// given
			GroupMemberListResponse response = new GroupMemberListResponse(
				List.of(new GroupMemberListItem(1L, 100L, "테스트유저", "https://example.com/profile.jpg", Instant.now())),
				new GroupMemberListResponse.PageInfo(null, 20, false));

			given(groupService.getGroupMembers(eq(1L), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/groups/{groupId}/members", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data[0].memberId").value(100))
				.andExpect(jsonPath("$.data.data[0].nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.page.hasNext").value(false));
		}
	}

	@Nested
	@DisplayName("그룹 멤버 강퇴")
	class DeleteGroupMember {

		@Test
		@DisplayName("그룹 멤버를 강퇴하면 성공 응답을 반환한다")
		void 그룹_멤버_강퇴_성공() throws Exception {
			// given
			willDoNothing().given(groupService).deleteGroupMember(1L, 100L);

			// when & then
			mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{userId}", 1L, 100L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 리뷰 목록 조회")
	class GetGroupReviews {

		@Test
		@DisplayName("그룹 리뷰를 조회하면 커서 페이징 결과를 반환한다")
		void 그룹_리뷰_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<ReviewResponse> response = new CursorPageResponse<>(
				List.of(new ReviewResponse(1L, 2L, 3L, "테스트그룹", "테스트하위그룹",
					new ReviewResponse.AuthorResponse("테스트유저"),
					"맛있어요", true, List.of("친절"),
					List.of(new ReviewResponse.ReviewImageResponse(1L, "https://example.com/review.jpg")),
					Instant.now(), null, null, null, null, null, null)),
				new CursorPageResponse.Pagination(null, false, 20));

			given(reviewService.getGroupReviews(eq(1L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/groups/{groupId}/reviews", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].author.nickname").value("테스트유저"))
				.andExpect(jsonPath("$.data.items[0].contentPreview").value("맛있어요"))
				.andExpect(jsonPath("$.data.pagination.hasNext").value(false));
		}
	}

	@Nested
	@DisplayName("그룹 리뷰 음식점 목록 조회")
	class GetGroupReviewRestaurants {

		@Test
		@DisplayName("그룹 리뷰가 있는 음식점 목록을 조회하면 커서 페이징 결과를 반환한다")
		void 그룹_리뷰_음식점_목록_조회_성공() throws Exception {
			// given
			CursorPageResponse<RestaurantListItem> response = new CursorPageResponse<>(
				List.of(new RestaurantListItem(
					1L,
					"맛집식당",
					"서울시 강남구",
					500.0,
					List.of("한식"),
					List.of(new RestaurantImageDto(1L, "https://example.com/img.jpg")),
					"맛집으로 유명한 식당입니다.")),
				new CursorPageResponse.Pagination(null, false, 20));

			given(restaurantService.getGroupRestaurants(eq(1L), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/groups/{groupId}/reviews/restaurants", 1L)
				.param("latitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LAT))
				.param("longitude", String.valueOf(RestaurantRequestFixture.DEFAULT_LNG))
				.param("radius", String.valueOf(RestaurantRequestFixture.DEFAULT_RADIUS))
				.param("size", String.valueOf(RestaurantRequestFixture.DEFAULT_SIZE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(1))
				.andExpect(jsonPath("$.items[0].name").value("맛집식당"))
				.andExpect(jsonPath("$.pagination.hasNext").value(false));
		}
	}
}
