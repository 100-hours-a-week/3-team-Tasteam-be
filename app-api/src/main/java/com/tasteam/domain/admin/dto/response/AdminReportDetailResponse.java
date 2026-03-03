package com.tasteam.domain.admin.dto.response;

import java.time.Instant;

import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;

public record AdminReportDetailResponse(
	Long id,
	Long memberId,
	String memberNickname,
	String memberEmail,
	ReportCategory category,
	String content,
	ReportStatus status,
	Instant createdAt,
	Instant updatedAt) {
}
