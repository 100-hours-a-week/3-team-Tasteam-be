package com.tasteam.domain.analytics.export;

public interface RawDataExportService {

	RawDataExportResult export(RawDataExportCommand command);
}
