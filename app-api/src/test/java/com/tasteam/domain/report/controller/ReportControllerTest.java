package com.tasteam.domain.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.report.dto.request.ReportCreateRequest;
import com.tasteam.domain.report.dto.response.ReportCreateResponse;
import com.tasteam.domain.report.dto.response.ReportListItem;
import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;
import com.tasteam.domain.report.service.ReportService;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;

@ControllerWebMvcTest(ReportController.class)
@DisplayName("[유닛](Report) ReportController 단위 테스트")
class ReportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ReportService reportService;

	@Nested
	@DisplayName("신고 등록")
	class Submit {

		@Test
		@DisplayName("신고를 등록하면 신고 정보가 생성되고 201을 반환한다")
		void 신고_등록_성공() throws Exception {
			// given
			var request = new ReportCreateRequest(ReportCategory.BUG, "지도 위치가 잘못됨");
			given(reportService.submit(anyLong(), any(ReportCreateRequest.class)))
				.willReturn(new ReportCreateResponse(100L, Instant.parse("2026-02-01T10:00:00Z")));

			// when & then
			mockMvc.perform(post("/api/v1/reports")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(100))
				.andExpect(jsonPath("$.data.createdAt").value("2026-02-01T10:00:00Z"));
		}

		@Test
		@DisplayName("카테고리가 없으면 400으로 요청 실패한다")
		void 신고_카테고리_미입력시_400() throws Exception {
			// given
			String body = "{}";

			// when & then
			mockMvc.perform(post("/api/v1/reports")
				.contentType(APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}

		@Test
		@DisplayName("회원 정보를 찾을 수 없으면 404로 실패한다")
		void 신고_등록_사용자_없음시_실패() throws Exception {
			// given
			var request = new ReportCreateRequest(ReportCategory.BUG, "오류 신고");
			given(reportService.submit(anyLong(), any()))
				.willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

			// when & then
			mockMvc.perform(post("/api/v1/reports")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("내 신고 목록 조회")
	class GetMyReports {

		@Test
		@DisplayName("신고 목록을 조회하면 페이징된 결과를 반환한다")
		void 내_신고_목록_조회_성공() throws Exception {
			// given
			var item = new ReportListItem(
				101L,
				ReportCategory.BUG,
				"지도 좌표 누락",
				ReportStatus.PENDING,
				Instant.parse("2026-02-01T10:10:00Z"));
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			given(reportService.getMyReports(anyLong(), any()))
				.willReturn(new PageImpl<>(List.of(item), pageable, 1));

			// when & then
			mockMvc.perform(get("/api/v1/reports/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content[0].id").value(101))
				.andExpect(jsonPath("$.data.content[0].category").value("BUG"))
				.andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
		}

		@Test
		@DisplayName("페이지 파라미터가 숫자가 아니어도 기본값으로 목록을 조회한다")
		void 내_신고_목록_페이지타입_오류_400() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/reports/me").param("page", "abc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}
}
