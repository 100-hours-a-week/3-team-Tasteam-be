package com.tasteam.domain.admin.dto.request;

import com.tasteam.domain.report.entity.ReportStatus;

import jakarta.validation.constraints.NotNull;

public record AdminReportStatusUpdateRequest(@NotNull
ReportStatus status) {
}
