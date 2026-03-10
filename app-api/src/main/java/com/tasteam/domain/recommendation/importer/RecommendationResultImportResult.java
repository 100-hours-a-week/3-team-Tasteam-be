package com.tasteam.domain.recommendation.importer;

/**
 * 추천 결과 ingest 실행 결과 요약.
 */
public record RecommendationResultImportResult(
	String modelVersion,
	long totalRows,
	long insertedRows,
	long skippedRows) {
}
