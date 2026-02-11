package com.tasteam.domain.announcement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.tasteam.domain.announcement.dto.response.AnnouncementDetailResponse;
import com.tasteam.domain.announcement.dto.response.AnnouncementListResponse;
import com.tasteam.domain.announcement.service.AnnouncementService;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;

@ControllerWebMvcTest(AnnouncementController.class)
class AnnouncementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AnnouncementService announcementService;

	private OffsetPageResponse<AnnouncementListResponse> createMockListResponse() {
		AnnouncementListResponse item = new AnnouncementListResponse(
			1L,
			"시스템 점검 안내",
			Instant.parse("2026-02-01T10:00:00Z"));

		return new OffsetPageResponse<>(
			List.of(item),
			new OffsetPagination(0, 10, 1, 1));
	}

	private AnnouncementDetailResponse createMockDetailResponse() {
		return new AnnouncementDetailResponse(
			1L,
			"시스템 점검 안내",
			"2월 15일 새벽 2시부터 4시까지 시스템 점검이 진행됩니다.",
			Instant.parse("2026-02-01T10:00:00Z"),
			Instant.parse("2026-02-01T10:00:00Z"));
	}

	@Nested
	@DisplayName("공지사항 목록 조회")
	class GetAnnouncementList {

		@Test
		@DisplayName("공지사항 목록을 조회하면 페이징된 결과를 반환한다")
		void getAnnouncementList_returnsPagedResult() throws Exception {
			given(announcementService.getAnnouncementList(any())).willReturn(createMockListResponse());

			mockMvc.perform(get("/api/v1/announcements")
				.param("page", "0")
				.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].title").value("시스템 점검 안내"))
				.andExpect(jsonPath("$.data.pagination.page").value(0))
				.andExpect(jsonPath("$.data.pagination.size").value(10));
		}
	}

	@Nested
	@DisplayName("공지사항 상세 조회")
	class GetAnnouncementDetail {

		@Test
		@DisplayName("공지사항 상세를 조회하면 상세 정보를 반환한다")
		void getAnnouncementDetail_returnsDetailInfo() throws Exception {
			given(announcementService.getAnnouncementDetail(anyLong())).willReturn(createMockDetailResponse());

			mockMvc.perform(get("/api/v1/announcements/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.title").value("시스템 점검 안내"))
				.andExpect(jsonPath("$.data.content").value("2월 15일 새벽 2시부터 4시까지 시스템 점검이 진행됩니다."));
		}
	}
}
