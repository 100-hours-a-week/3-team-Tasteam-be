package com.tasteam.batch.ai.vector.service;

import com.tasteam.infra.ai.dto.AiVectorUploadResponse;

/**
 * 벡터 업로드 API 호출 결과. 성공/실패 분기용.
 */
public sealed interface VectorUploadInvokeResult
	permits VectorUploadInvokeResult.Success, VectorUploadInvokeResult.Failure {

	record Success(AiVectorUploadResponse response) implements VectorUploadInvokeResult {
	}

	record Failure(Exception cause) implements VectorUploadInvokeResult {
	}
}
