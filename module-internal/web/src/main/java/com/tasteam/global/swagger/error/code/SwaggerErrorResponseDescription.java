package com.tasteam.global.swagger.error.code;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;

/**
 * 각 API 별 에러 응답 정의를 위해 도메인별 Enum이 구현하는 인터페이스.
 */
public interface SwaggerErrorResponseDescription {

	Set<ErrorCode> getErrorCodeList();

	default Set<ErrorCode> withCommonErrors(Set<ErrorCode> errorCodes) {
		Set<ErrorCode> merged = new LinkedHashSet<>(errorCodes);
		merged.add(CommonErrorCode.INTERNAL_SERVER_ERROR);
		return merged;
	}
}
