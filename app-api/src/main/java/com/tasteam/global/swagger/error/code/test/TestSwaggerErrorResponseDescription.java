package com.tasteam.global.swagger.error.code.test;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum TestSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	TEST_BUSINESS_EXCEPTION(new LinkedHashSet<>(Set.of(
		MemberErrorCode.MEMBER_NOT_FOUND))),
	TEST_SYSTEM_EXCEPTION(new LinkedHashSet<>());

	private final Set<ErrorCode> errorCodeList;

	TestSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
