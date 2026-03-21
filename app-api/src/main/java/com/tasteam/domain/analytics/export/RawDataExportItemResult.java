package com.tasteam.domain.analytics.export;

public record RawDataExportItemResult(
	RawDataType type,
	int rowCount,
	String dataObjectKey,
	String successObjectKey,
	boolean replacedExisting) {
}
