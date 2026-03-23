package com.tasteam.domain.admin.dto.request;

import java.time.LocalDate;
import java.util.Set;

import com.tasteam.domain.analytics.export.RawDataType;

public record AdminRawDataExportRequest(
	LocalDate dt,
	Set<RawDataType> targets,
	String requestId) {
}
