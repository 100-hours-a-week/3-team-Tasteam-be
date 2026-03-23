package com.tasteam.domain.analytics.export;

import java.time.LocalDate;
import java.util.Set;

public record RawDataExportCommand(
	LocalDate dt,
	Set<RawDataType> targets,
	String requestId) {
}
