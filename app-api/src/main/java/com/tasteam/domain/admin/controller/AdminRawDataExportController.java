package com.tasteam.domain.admin.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.controller.docs.AdminRawDataExportControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminRawDataExportRequest;
import com.tasteam.domain.analytics.export.RawDataExportCommand;
import com.tasteam.domain.analytics.export.RawDataExportResult;
import com.tasteam.domain.analytics.export.RawDataExportService;
import com.tasteam.domain.analytics.export.RawDataType;
import com.tasteam.global.dto.api.SuccessResponse;

@Profile({"dev", "stg"})
@RestController
@RequestMapping("/api/v1/admin/analytics/raw-exports")
public class AdminRawDataExportController implements AdminRawDataExportControllerDocs {

	private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

	private final RawDataExportService rawDataExportService;

	public AdminRawDataExportController(RawDataExportService rawDataExportService) {
		this.rawDataExportService = rawDataExportService;
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<RawDataExportResult> runRawExport(@RequestBody(required = false)
	AdminRawDataExportRequest request) {
		LocalDate dt = request == null || request.dt() == null ? LocalDate.now(KST_ZONE) : request.dt();
		Set<RawDataType> targets = request == null || request.targets() == null || request.targets().isEmpty()
			? EnumSet.allOf(RawDataType.class)
			: request.targets();
		String requestId = request == null || request.requestId() == null || request.requestId().isBlank()
			? "admin-raw-export-" + UUID.randomUUID()
			: request.requestId();

		RawDataExportResult result = rawDataExportService.export(new RawDataExportCommand(dt, targets, requestId));
		return SuccessResponse.success(result);
	}
}
