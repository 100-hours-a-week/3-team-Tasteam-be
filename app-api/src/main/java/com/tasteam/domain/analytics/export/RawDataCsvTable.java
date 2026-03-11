package com.tasteam.domain.analytics.export;

import java.util.List;

record RawDataCsvTable(
	List<String> headers,
	List<List<String>> rows) {

	int rowCount() {
		return rows == null ? 0 : rows.size();
	}
}
