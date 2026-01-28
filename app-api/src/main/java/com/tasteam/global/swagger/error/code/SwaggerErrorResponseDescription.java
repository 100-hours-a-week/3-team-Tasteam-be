package com.tasteam.global.swagger.error.code;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;

/**
 * Swagger API 응답 설명을 위한 공통 인터페이스
 * 각 도메인별 Enum에서 구현한다.
 */
public interface SwaggerErrorResponseDescription {

	Set<ErrorCode> getErrorCodeList();

	default Set<ErrorCode> withCommonErrors(Set<ErrorCode> errorCodes) {
		Set<ErrorCode> merged = new LinkedHashSet<>(errorCodes);
		merged.addAll(Set.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
		return merged;
	}
}
