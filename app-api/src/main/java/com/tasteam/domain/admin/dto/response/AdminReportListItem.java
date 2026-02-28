package com.tasteam.domain.admin.dto.response;

import java.time.Instant;

import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;

public record AdminReportListItem(
	Long id,
	String memberNickname,
	ReportCategory category,
	String contentPreview,
	ReportStatus status,
	Instant createdAt) {
}
