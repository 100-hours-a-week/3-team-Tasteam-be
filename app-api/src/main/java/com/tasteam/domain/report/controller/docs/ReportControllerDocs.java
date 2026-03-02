package com.tasteam.domain.report.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.tasteam.domain.report.dto.request.ReportCreateRequest;
import com.tasteam.domain.report.dto.response.ReportCreateResponse;
import com.tasteam.domain.report.dto.response.ReportListItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(85)
@Tag(name = "Report", description = "신고/피드백 API")
public interface ReportControllerDocs {

	@Operation(summary = "신고 접수", description = "카테고리를 선택하고 신고를 접수합니다.")
	ResponseEntity<SuccessResponse<ReportCreateResponse>> submit(
		ReportCreateRequest request,
		@CurrentUser
		Long memberId);

	@Operation(summary = "내 신고 내역 조회", description = "본인이 접수한 신고 목록을 페이지 단위로 조회합니다.")
	SuccessResponse<Page<ReportListItem>> getMyReports(
		@CurrentUser
		Long memberId,
		Pageable pageable);
}
