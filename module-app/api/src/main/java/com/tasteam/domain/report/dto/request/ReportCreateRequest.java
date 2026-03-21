package com.tasteam.domain.report.dto.request;

import com.tasteam.domain.report.entity.ReportCategory;

import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
	@NotNull
	ReportCategory category,
	String content) {
}
