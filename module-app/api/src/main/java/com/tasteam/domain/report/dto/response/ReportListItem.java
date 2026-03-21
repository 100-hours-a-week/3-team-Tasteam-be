package com.tasteam.domain.report.dto.response;

import java.time.Instant;

import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;

public record ReportListItem(
	Long id,
	ReportCategory category,
	String content,
	ReportStatus status,
	Instant createdAt) {
}
