package com.tasteam.batch.ai.review.runner;

/**
 * 감정/요약/비교 분석 Runner 실행 결과. 워커가 완료/실패 분기 시 사용.
 */
public sealed interface AnalysisRunResult
	permits AnalysisRunResult.Success, AnalysisRunResult.Failure {

	record Success() implements AnalysisRunResult {
	}

	record Failure(Exception cause) implements AnalysisRunResult {
	}
}
