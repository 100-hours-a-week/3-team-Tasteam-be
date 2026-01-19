package com.tasteam.core.swagger.error.code;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.core.exception.ErrorCode;
import com.tasteam.core.exception.code.CommonErrorCode;

import lombok.Getter;

/**
 * Swagger API 응답 설명을 위한 Enum
 * 각 API별로 발생 가능한 에러 코드를 정의
 */
@Getter
public enum SwaggerErrorResponseDescription {

	/* 예시
	AUTH_SIGNUP(new LinkedHashSet<>(Set.of(
		MemberErrorCode.INVALID_EMAIL_FORMAT,
		MemberErrorCode.INVALID_PASSWORD_FORMAT,
		MemberErrorCode.INVALID_NICKNAME,
		MemberErrorCode.DUPLICATE_EMAIL,
		MemberErrorCode.DUPLICATE_NICKNAME)))
	 */
	;

	private final Set<ErrorCode> errorCodeList;

	SwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		// 공통 에러 추가
		errorCodeList.addAll(new LinkedHashSet<>(Set.of(
			CommonErrorCode.INTERNAL_SERVER_ERROR)));

		this.errorCodeList = errorCodeList;
	}
}
