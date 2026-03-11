package com.tasteam.domain.analytics.export;

import java.time.LocalDate;

public interface RawDataExportRuntimeDiagnostics {

	void logSnapshot(String phase, RawDataExportCommand command, LocalDate dt, int totalJobs, int successCount);

	static RawDataExportRuntimeDiagnostics noop() {
		return (phase, command, dt, totalJobs, successCount) -> {};
	}
}
