package com.tasteam.domain.admin.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.admin.dto.request.AdminReportStatusUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminReportDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminReportListItem;
import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(140)
@Tag(name = "Admin - Report", description = "어드민 신고 관리 API")
public interface AdminReportControllerDocs {

	@Operation(summary = "신고 목록 조회", description = "전체 신고를 최신순으로 조회합니다. category/status로 필터링할 수 있습니다.")
	SuccessResponse<Page<AdminReportListItem>> getReports(
		@Parameter(description = "카테고리 필터 (선택)") @RequestParam(required = false)
		ReportCategory category,
		@Parameter(description = "처리 상태 필터 (선택)") @RequestParam(required = false)
		ReportStatus status,
		Pageable pageable);

	@Operation(summary = "신고 상세 조회", description = "신고 ID로 상세 정보를 조회합니다.")
	SuccessResponse<AdminReportDetailResponse> getDetail(
		@Parameter(description = "신고 ID", example = "1") @PathVariable
		Long reportId);

	@Operation(summary = "신고 처리 상태 변경", description = "신고의 처리 상태를 변경합니다.")
	void updateStatus(
		@Parameter(description = "신고 ID", example = "1") @PathVariable
		Long reportId,
		AdminReportStatusUpdateRequest request);
}
