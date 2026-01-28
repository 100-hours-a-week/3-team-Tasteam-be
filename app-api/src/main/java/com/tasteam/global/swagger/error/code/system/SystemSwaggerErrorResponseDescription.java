package com.tasteam.global.swagger.error.code.system;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum SystemSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	HEALTH_CHECK(new LinkedHashSet<>());

	private final Set<ErrorCode> errorCodeList;

	SystemSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
