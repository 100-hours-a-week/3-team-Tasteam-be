package com.tasteam.batch.ai.vector.runner;

/**
 * 벡터 업로드 Runner 실행 결과. 워커가 완료/실패/스킵 분기 시 사용.
 */
public sealed interface VectorUploadRunResult
	permits VectorUploadRunResult.Success, VectorUploadRunResult.DataMissing,
	VectorUploadRunResult.InvokeFailed, VectorUploadRunResult.SyncSkipped {

	/** 에폭 싱크까지 성공. 갱신된 vector_epoch 반환. */
	record Success(long newVectorEpoch) implements VectorUploadRunResult {
	}

	/** 레스토랑 없음/삭제됨 → Job STALE 처리 */
	record DataMissing() implements VectorUploadRunResult {
	}

	/** AI 호출 실패 → Job FAILED 처리 */
	record InvokeFailed(Exception cause) implements VectorUploadRunResult {
	}

	/** 에폭 싱크 스킵(동시성) → 별도 처리 없음 */
	record SyncSkipped() implements VectorUploadRunResult {
	}
}
