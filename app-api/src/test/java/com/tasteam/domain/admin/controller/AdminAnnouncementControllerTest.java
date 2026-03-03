package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.admin.dto.request.AdminAnnouncementCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminAnnouncementUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementListItem;
import com.tasteam.domain.admin.service.AdminAnnouncementService;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.PromotionErrorCode;

@ControllerWebMvcTest(AdminAnnouncementController.class)
@DisplayName("[유닛](Admin) AdminAnnouncementController 단위 테스트")
class AdminAnnouncementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminAnnouncementService adminAnnouncementService;

	@Nested
	@DisplayName("공지사항 목록 조회")
	class GetAnnouncements {

		@Test
		@DisplayName("공개 전용 공지사항 목록 조회 시 페이징된 결과를 반환한다")
		void 공지사항_목록_조회_성공() throws Exception {
			// given
			AdminAnnouncementListItem item = new AdminAnnouncementListItem(
				1L,
				"점검 안내",
				Instant.parse("2026-02-01T10:00:00Z"),
				Instant.parse("2026-02-01T10:10:00Z"));
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			Page<AdminAnnouncementListItem> page = new PageImpl<>(List.of(item), pageable, 1);
			given(adminAnnouncementService.getAnnouncementList(any(Pageable.class))).willReturn(page);

			// when & then
			mockMvc.perform(get("/api/v1/admin/announcements"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content[0].id").value(1))
				.andExpect(jsonPath("$.data.content[0].title").value("점검 안내"));
		}

		@Test
		@DisplayName("페이지 파라미터가 숫자가 아니면 기본값으로 조회를 수행한다")
		void 공지사항_목록_페이지타입_오류_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/announcements").param("page", "abc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("공지사항 상세 조회")
	class GetAnnouncement {

		@Test
		@DisplayName("공지사항 ID로 상세 정보를 조회한다")
		void 공지사항_상세_조회_성공() throws Exception {
			// given
			given(adminAnnouncementService.getAnnouncementDetail(1L)).willReturn(new AdminAnnouncementDetailResponse(
				1L,
				"점검 안내",
				"새벽 2시 점검 예정입니다.",
				Instant.parse("2026-02-01T10:00:00Z"),
				Instant.parse("2026-02-01T10:10:00Z")));

			// when & then
			mockMvc.perform(get("/api/v1/admin/announcements/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.title").value("점검 안내"))
				.andExpect(jsonPath("$.data.content").value("새벽 2시 점검 예정입니다."));
		}

		@Test
		@DisplayName("존재하지 않는 공지사항이면 404로 실패한다")
		void 공지사항_상세_미존재_실패() throws Exception {
			// given
			given(adminAnnouncementService.getAnnouncementDetail(999L))
				.willThrow(new BusinessException(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/v1/admin/announcements/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND.name()));
		}
	}

	@Nested
	@DisplayName("공지사항 생성")
	class CreateAnnouncement {

		@Test
		@DisplayName("유효한 요청이면 공지사항을 생성하고 ID를 반환한다")
		void 공지사항_생성_성공() throws Exception {
			// given
			var request = new AdminAnnouncementCreateRequest("점검 안내", "서비스 점검이 진행됩니다.");
			given(adminAnnouncementService.createAnnouncement(request)).willReturn(10L);

			// when & then
			mockMvc.perform(post("/api/v1/admin/announcements")
				.contentType(APPLICATION_JSON)
				.content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(10));
		}

		@Test
		@DisplayName("제목 누락이면 400으로 실패한다")
		void 공지사항_생성_제목_누락_실패() throws Exception {
			// given
			String body = "{\"content\":\"내용\"}";

			// when & then
			mockMvc.perform(post("/api/v1/admin/announcements")
				.contentType(APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Nested
	@DisplayName("공지사항 수정")
	class UpdateAnnouncement {

		@Test
		@DisplayName("수정 요청 시 성공적으로 업데이트한다")
		void 공지사항_수정_성공() throws Exception {
			// given
			var request = new AdminAnnouncementUpdateRequest("수정 제목", "수정 내용");
			doNothing().when(adminAnnouncementService).updateAnnouncement(1L, request);

			// when & then
			mockMvc.perform(patch("/api/v1/admin/announcements/1")
				.contentType(APPLICATION_JSON)
				.content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("존재하지 않는 공지사항은 수정할 수 없다")
		void 공지사항_수정_미존재_실패() throws Exception {
			// given
			var request = new AdminAnnouncementUpdateRequest("제목", "내용");
			willThrow(new BusinessException(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND))
				.given(adminAnnouncementService)
				.updateAnnouncement(999L, request);
			// when & then
			mockMvc.perform(patch("/api/v1/admin/announcements/999")
				.contentType(APPLICATION_JSON)
				.content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND.name()));
		}
	}

	@Nested
	@DisplayName("공지사항 삭제")
	class DeleteAnnouncement {

		@Test
		@DisplayName("존재하는 공지사항을 삭제하면 본문 없이 204를 반환한다")
		void 공지사항_삭제_성공() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/v1/admin/announcements/1"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("존재하지 않는 공지사항 삭제 시 404로 실패한다")
		void 공지사항_삭제_미존재_실패() throws Exception {
			// given
			willThrow(new BusinessException(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND))
				.given(adminAnnouncementService)
				.deleteAnnouncement(999L);

			// when & then
			mockMvc.perform(delete("/api/v1/admin/announcements/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND.name()));
		}
	}
}
