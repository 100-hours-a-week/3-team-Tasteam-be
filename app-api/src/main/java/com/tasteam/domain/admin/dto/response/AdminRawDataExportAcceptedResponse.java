package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.tasteam.domain.analytics.export.RawDataType;

public record AdminRawDataExportAcceptedResponse(
	String requestId,
	LocalDate dt,
	Set<RawDataType> targets,
	Instant acceptedAt) {
}
