package com.tasteam.global.swagger.error.code.auth;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum AuthSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	AUTH_TOKEN_REFRESH(new LinkedHashSet<>(Set.of(
		AuthErrorCode.AUTHENTICATION_REQUIRED,
		AuthErrorCode.REFRESH_TOKEN_NOT_FOUND,
		AuthErrorCode.REFRESH_TOKEN_INVALID,
		AuthErrorCode.REFRESH_TOKEN_EXPIRED,
		AuthErrorCode.REFRESH_TOKEN_REUSED,
		MemberErrorCode.MEMBER_NOT_FOUND,
		MemberErrorCode.MEMBER_INACTIVE))),

	AUTH_LOGOUT(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED))),

	LOCAL_TOKEN_ISSUE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST)));

	private final Set<ErrorCode> errorCodeList;

	AuthSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
