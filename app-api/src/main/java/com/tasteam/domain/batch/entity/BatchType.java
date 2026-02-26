package com.tasteam.domain.batch.entity;

/**
 * 배치 실행 유형.
 * - VECTOR_UPLOAD_DAILY: 벡터 업로드 (24시간 주기)
 * - REVIEW_ANALYSIS_DAILY: 감정/요약 분석 (벡터 성공 대상)
 * - RESTAURANT_COMPARISON_WEEKLY: 음식점 비교 (1주일 주기)
 */
public enum BatchType {
	VECTOR_UPLOAD_DAILY,
	REVIEW_ANALYSIS_DAILY,
	RESTAURANT_COMPARISON_WEEKLY
}
