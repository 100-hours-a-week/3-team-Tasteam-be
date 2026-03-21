package com.tasteam.domain.analytics.export;

import java.time.LocalDate;
import java.util.List;

public record RawDataExportResult(
	LocalDate dt,
	List<RawDataExportItemResult> items) {
}
