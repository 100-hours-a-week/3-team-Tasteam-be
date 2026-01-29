package com.tasteam.domain.subgroup.controller;

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
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.service.SubgroupService;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;

@ControllerWebMvcTest(SubgroupManagementController.class)
class SubgroupManagementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private SubgroupService subgroupService;

	@Nested
	@DisplayName("내 서브그룹 목록 조회")
	class GetMySubgroups {

		@Test
		@DisplayName("내 서브그룹 목록을 조회하면 서브그룹 리스트를 반환한다")
		void 내_서브그룹_목록_조회_성공() throws Exception {
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

			SubgroupListResponse response = new SubgroupListResponse(
				List.of(item),
				new SubgroupListResponse.PageInfo("name", null, 20, false));

			given(subgroupService.getGroupSubgroups(eq(1L), any(), any(), any(), any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/groups/{groupId}/subgroups", 1L)
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.data[0].subgroupId").value(1))
				.andExpect(jsonPath("$.data.data[0].name").value("서브그룹1"));
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

			given(subgroupService.getSubgroup(eq(10L), any())).willReturn(response);

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
	@DisplayName("서브그룹 탈퇴")
	class WithdrawSubgroup {

		@Test
		@DisplayName("서브그룹에서 탈퇴하면 204를 반환한다")
		void 서브그룹_탈퇴_성공() throws Exception {
			// given
			willDoNothing().given(subgroupService).withdrawSubgroup(eq(10L), any());

			// when & then
			mockMvc.perform(delete("/api/v1/subgroups/{subgroupId}/members/me", 10L))
				.andExpect(status().isNoContent());
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
}
