package com.tasteam.global.swagger.error.code;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;

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
	MEMBER_ME(new LinkedHashSet<>(Set.of(
		MemberErrorCode.MEMBER_NOT_FOUND))),
	MEMBER_PROFILE_UPDATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		MemberErrorCode.MEMBER_NOT_FOUND,
		MemberErrorCode.EMAIL_ALREADY_EXISTS))),
	MEMBER_WITHDRAW(new LinkedHashSet<>(Set.of(
		MemberErrorCode.MEMBER_NOT_FOUND))),
	AUTH_TOKEN_REFRESH(new LinkedHashSet<>(Set.of(
		AuthErrorCode.AUTHENTICATION_REQUIRED,
		AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
		AuthErrorCode.REFRESH_TOKEN_INVALID,
		AuthErrorCode.REFRESH_TOKEN_EXPIRED,
		AuthErrorCode.REFRESH_TOKEN_REUSED,
		MemberErrorCode.MEMBER_NOT_FOUND,
		MemberErrorCode.MEMBER_INACTIVE))),
		;

	private final Set<ErrorCode> errorCodeList;

	SwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		// 공통 에러 추가
		errorCodeList.addAll(new LinkedHashSet<>(Set.of(
			CommonErrorCode.INTERNAL_SERVER_ERROR)));

		this.errorCodeList = errorCodeList;
	}
}
