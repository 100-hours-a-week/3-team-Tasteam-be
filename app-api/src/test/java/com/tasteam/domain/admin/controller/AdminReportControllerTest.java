package com.tasteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.tasteam.domain.admin.dto.request.AdminReportStatusUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminReportDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminReportListItem;
import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.ReportErrorCode;

@DisplayName("[유닛](Admin) AdminReportController 단위 테스트")
class AdminReportControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("신고 목록 조회")
	class GetReports {

		@Test
		@DisplayName("신고 목록을 조회하면 페이징 결과를 반환한다")
		void 신고_목록_조회_성공() throws Exception {
			// given
			AdminReportListItem item = new AdminReportListItem(
				1L,
				"사용자A",
				ReportCategory.BUG,
				"지도 좌표가 누락되었습니다.",
				ReportStatus.PENDING,
				Instant.parse("2026-02-01T10:00:00Z"));
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			Page<AdminReportListItem> page = new PageImpl<>(List.of(item), pageable, 1);
			given(adminReportService.getReports(any(), any(), any())).willReturn(page);

			// when & then
			mockMvc.perform(get("/api/v1/admin/reports"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.content[0].id").value(1))
				.andExpect(jsonPath("$.data.content[0].category").value("BUG"));
		}

		@Test
		@DisplayName("페이지 파라미터 타입이 잘못되어도 기본값으로 목록 조회를 성공한다")
		void 신고_목록_페이지타입_오류_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/reports").param("page", "abc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		}
	}

	@Nested
	@DisplayName("신고 상세 조회")
	class GetReportDetail {

		@Test
		@DisplayName("신고 ID로 상세 정보를 조회한다")
		void 신고_상세_조회_성공() throws Exception {
			// given
			given(adminReportService.getDetail(1L)).willReturn(new AdminReportDetailResponse(
				1L,
				100L,
				"사용자A",
				"user@example.com",
				ReportCategory.BUG,
				"지도 표시가 틀립니다.",
				ReportStatus.PENDING,
				Instant.parse("2026-02-01T10:00:00Z"),
				Instant.parse("2026-02-01T10:10:00Z")));

			// when & then
			mockMvc.perform(get("/api/v1/admin/reports/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.status").value("PENDING"));
		}

		@Test
		@DisplayName("신고가 없으면 404로 실패한다")
		void 신고_상세_미존재_실패() throws Exception {
			// given
			given(adminReportService.getDetail(999L))
				.willThrow(new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/v1/admin/reports/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(ReportErrorCode.REPORT_NOT_FOUND.name()));
		}
	}

	@Nested
	@DisplayName("신고 처리 상태 변경")
	class UpdateStatus {

		@Test
		@DisplayName("신고 처리 상태를 업데이트하면 204를 반환한다")
		void 신고_상태_변경_성공() throws Exception {
			// given
			var request = new AdminReportStatusUpdateRequest(ReportStatus.RESOLVED);
			doNothing().when(adminReportService).updateStatus(1L, request.status());

			// when & then
			mockMvc.perform(patch("/api/v1/admin/reports/1/status")
				.contentType(APPLICATION_JSON)
				.content("""
					{"status":"RESOLVED"}
					"""))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		}

		@Test
		@DisplayName("요청 바디가 비면 400으로 실패한다")
		void 신고_상태_변경_요청_누락_실패() throws Exception {
			// when & then
			mockMvc.perform(patch("/api/v1/admin/reports/1/status")
				.contentType(APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}
}
