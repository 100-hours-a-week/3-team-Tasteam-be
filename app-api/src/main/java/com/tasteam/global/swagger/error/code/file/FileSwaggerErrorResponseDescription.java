package com.tasteam.global.swagger.error.code.file;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum FileSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	PRESIGNED_UPLOADS(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST))),
	DOMAIN_LINK(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		FileErrorCode.FILE_NOT_FOUND,
		FileErrorCode.FILE_CONFLICT))),
	IMAGE_DETAIL(new LinkedHashSet<>(Set.of(
		FileErrorCode.FILE_NOT_FOUND))),
	IMAGE_SUMMARY(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST)));

	private final Set<ErrorCode> errorCodeList;

	FileSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
