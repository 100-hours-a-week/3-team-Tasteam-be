package com.tasteam.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.dto.response.MemberPreviewResponse;
import com.tasteam.domain.member.dto.response.MemberSubgroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberSummaryResponse;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.service.SubgroupService;

@ControllerWebMvcTest(MemberController.class)
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private SubgroupService subgroupService;

	@Nested
	@DisplayName("내 정보 조회")
	class GetMyMemberInfo {

		@Test
		@DisplayName("내 정보를 조회하면 프로필 정보를 반환한다")
		void 내_정보_조회_성공() throws Exception {
			// given
			MemberMeResponse response = new MemberMeResponse(
				new MemberSummaryResponse("테스트유저", "https://example.com/profile.jpg"),
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

			given(subgroupService.getMySubgroups(eq(1L), any(), any(), any(), any())).willReturn(response);

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

			MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(
				"new@example.com", "https://example.com/new-profile.jpg");

			// when & then
			mockMvc.perform(patch("/api/v1/members/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
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
}
