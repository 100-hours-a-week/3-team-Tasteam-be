package com.tasteam.global.swagger.error.code.search;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.SearchErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum SearchSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	SEARCH(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST))),
	RECENT_SEARCH_LIST(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST))),
	RECENT_SEARCH_DELETE(new LinkedHashSet<>(Set.of(
		SearchErrorCode.RECENT_SEARCH_NOT_FOUND)));

	private final Set<ErrorCode> errorCodeList;

	SearchSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
