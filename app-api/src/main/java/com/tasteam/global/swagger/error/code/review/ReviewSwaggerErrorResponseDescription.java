package com.tasteam.global.swagger.error.code.review;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.ReviewErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum ReviewSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	REVIEW_KEYWORDS(new LinkedHashSet<>(Set.of(
		ReviewErrorCode.KEYWORD_NOT_FOUND))),
	REVIEW_DETAIL(new LinkedHashSet<>(Set.of(
		ReviewErrorCode.REVIEW_NOT_FOUND))),
	REVIEW_DELETE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.NO_PERMISSION,
		ReviewErrorCode.REVIEW_NOT_FOUND)));

	private final Set<ErrorCode> errorCodeList;

	ReviewSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
