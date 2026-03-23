package com.tasteam.domain.search.repository.executor;

import com.tasteam.domain.search.dto.SearchCursor;

/**
 * 검색 결과 커서 점수 계산 유틸리티.
 * <p>
 * QueryDSL 전략(SearchQueryExpressions)과 Native SQL 전략(NativeSearchExecutorSupport)에서
 * 동일한 점수 공식을 사용하도록 단일 구현으로 통합한다.
 */
public final class SearchScoreCalculator {

	private SearchScoreCalculator() {}

	/**
	 * FTS 제외 커서 점수 계산.
	 * 공식: name_exact * 100 + name_similarity * 30 + distance_weight * 50
	 */
	public static double cursorScore(SearchCursor cursor, Double radiusMeters) {
		double nameExact = cursor.nameExact() == null ? 0.0 : cursor.nameExact();
		double similarity = cursor.nameSimilarity() == null ? 0.0 : cursor.nameSimilarity();
		return nameExact * 100.0 + similarity * 30.0
			+ distanceWeight(cursor.distanceMeters(), radiusMeters) * 50.0;
	}

	/**
	 * FTS 포함 커서 점수 계산.
	 * 공식: name_exact * 100 + name_similarity * 30 + fts_rank * 25 + category * 15 + address * 5 + distance_weight * 50
	 */
	public static double cursorScoreFts(SearchCursor cursor, Double radiusMeters) {
		double nameExact = cursor.nameExact() == null ? 0.0 : cursor.nameExact();
		double similarity = cursor.nameSimilarity() == null ? 0.0 : cursor.nameSimilarity();
		double ftsRank = cursor.ftsRank() == null ? 0.0 : cursor.ftsRank();
		double categoryMatch = cursor.categoryMatch() == null ? 0.0 : cursor.categoryMatch();
		double addressMatch = cursor.addressMatch() == null ? 0.0 : cursor.addressMatch();
		return nameExact * 100.0 + similarity * 30.0 + ftsRank * 25.0
			+ categoryMatch * 15.0 + addressMatch * 5.0
			+ distanceWeight(cursor.distanceMeters(), radiusMeters) * 50.0;
	}

	private static double distanceWeight(Double distanceMeters, Double radiusMeters) {
		if (distanceMeters == null || radiusMeters == null) {
			return 0.0;
		}
		return Math.max(0.0, 1.0 - (distanceMeters / Math.max(radiusMeters, 1.0)));
	}
}
