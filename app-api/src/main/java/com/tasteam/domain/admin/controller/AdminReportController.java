package com.tasteam.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.controller.docs.AdminReportControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminReportStatusUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminReportDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminReportListItem;
import com.tasteam.domain.admin.service.AdminReportService;
import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
@Validated
public class AdminReportController implements AdminReportControllerDocs {

	private final AdminReportService adminReportService;

	@Override
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Page<AdminReportListItem>> getReports(
		@RequestParam(required = false)
		ReportCategory category,
		@RequestParam(required = false)
		ReportStatus status,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable) {

		return SuccessResponse.success(adminReportService.getReports(category, status, pageable));
	}

	@Override
	@GetMapping("/{reportId}")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminReportDetailResponse> getDetail(
		@PathVariable
		Long reportId) {

		return SuccessResponse.success(adminReportService.getDetail(reportId));
	}

	@Override
	@PatchMapping("/{reportId}/status")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateStatus(
		@PathVariable
		Long reportId,
		@RequestBody @Valid
		AdminReportStatusUpdateRequest request) {

		adminReportService.updateStatus(reportId, request.status());
	}
}
