package com.tasteam.global.swagger.error.code.main;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum MainSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	MAIN_PAGE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST)));

	private final Set<ErrorCode> errorCodeList;

	MainSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
