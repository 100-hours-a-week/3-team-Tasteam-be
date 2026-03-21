package com.tasteam.domain.report.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.report.controller.docs.ReportControllerDocs;
import com.tasteam.domain.report.dto.request.ReportCreateRequest;
import com.tasteam.domain.report.dto.response.ReportCreateResponse;
import com.tasteam.domain.report.dto.response.ReportListItem;
import com.tasteam.domain.report.service.ReportService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Validated
public class ReportController implements ReportControllerDocs {

	private final ReportService reportService;

	@Override
	@PreAuthorize("hasRole('USER')")
	@PostMapping
	public ResponseEntity<SuccessResponse<ReportCreateResponse>> submit(
		@RequestBody @Valid
		ReportCreateRequest request,
		@CurrentUser
		Long memberId) {

		ReportCreateResponse response = reportService.submit(memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
	}

	@Override
	@PreAuthorize("hasRole('USER')")
	@GetMapping("/me")
	public SuccessResponse<Page<ReportListItem>> getMyReports(
		@CurrentUser
		Long memberId,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable) {

		return SuccessResponse.success(reportService.getMyReports(memberId, pageable));
	}
}
