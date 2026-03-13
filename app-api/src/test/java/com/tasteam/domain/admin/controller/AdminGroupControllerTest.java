package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.tasteam.domain.admin.dto.request.AdminGroupCreateRequest;
import com.tasteam.domain.admin.dto.response.AdminGroupListItem;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.fixture.AdminGroupRequestFixture;

@DisplayName("[유닛](Admin) AdminGroupController 단위 테스트")
class AdminGroupControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("그룹 목록 조회")
	class GetGroups {

		@Test
		@DisplayName("그룹 목록 조회 시 페이징된 결과를 반환한다")
		void 그룹_목록_조회_성공() throws Exception {
			// given
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			Page<AdminGroupListItem> page = new PageImpl<>(List.of(
				new AdminGroupListItem(
					1L,
					"강남동호회",
					GroupType.UNOFFICIAL,
					"서울 강남구",
					GroupJoinType.PASSWORD,
					null,
					com.tasteam.domain.group.type.GroupStatus.ACTIVE,
					null)),
				pageable,
				1);
			given(adminGroupService.getGroups(any(Pageable.class))).willReturn(page);

			// when & then
			mockMvc.perform(get("/api/v1/admin/groups").param("page", "0").param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content[0].id").value(1))
				.andExpect(jsonPath("$.data.content[0].name").value("강남동호회"));
		}

		@Test
		@DisplayName("페이지 파라미터가 숫자가 아니어도 기본값으로 목록을 조회한다")
		void 그룹_목록_조회_페이지타입_오류_기본값_조회() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/groups").param("page", "abc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("그룹 생성")
	class CreateGroup {

		@Test
		@DisplayName("유효한 요청이면 그룹 ID를 반환한다")
		void 그룹_생성_성공() throws Exception {
			// given
			var request = AdminGroupRequestFixture.createRequest();
			given(adminGroupService.createGroup(any(AdminGroupCreateRequest.class))).willReturn(10L);

			// when & then
			mockMvc.perform(post("/api/v1/admin/groups")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(10));
		}

		@Test
		@DisplayName("필수 값이 누락되면 400으로 실패한다")
		void 그룹_생성_필수값_누락_실패() throws Exception {
			// given
			var request = new AdminGroupCreateRequest("", null, GroupType.UNOFFICIAL, "", null, GroupJoinType.PASSWORD,
				null);

			// when & then
			mockMvc.perform(post("/api/v1/admin/groups")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}
}
