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
		CommonErrorCode.INVALID_REQUEST,
		CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
		CommonErrorCode.EXTERNAL_SERVICE_TIMEOUT,
		FileErrorCode.STORAGE_ACCESS_DENIED,
		FileErrorCode.STORAGE_INVALID_CREDENTIALS,
		FileErrorCode.STORAGE_SIGNATURE_MISMATCH,
		FileErrorCode.STORAGE_TOKEN_EXPIRED,
		FileErrorCode.STORAGE_REQUEST_EXPIRED,
		FileErrorCode.STORAGE_BUCKET_NOT_FOUND,
		FileErrorCode.STORAGE_BUCKET_INVALID,
		FileErrorCode.STORAGE_INVALID_REQUEST,
		FileErrorCode.STORAGE_ENTITY_TOO_LARGE,
		FileErrorCode.STORAGE_ENTITY_TOO_SMALL,
		FileErrorCode.STORAGE_THROTTLED,
		FileErrorCode.STORAGE_SERVICE_UNAVAILABLE,
		FileErrorCode.STORAGE_INTERNAL_ERROR,
		FileErrorCode.STORAGE_ERROR))),
	DOMAIN_LINK(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		FileErrorCode.FILE_NOT_FOUND,
		FileErrorCode.FILE_CONFLICT))),
	IMAGE_DETAIL(new LinkedHashSet<>(Set.of(
		FileErrorCode.FILE_NOT_FOUND,
		CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
		CommonErrorCode.EXTERNAL_SERVICE_TIMEOUT,
		FileErrorCode.STORAGE_ACCESS_DENIED,
		FileErrorCode.STORAGE_INVALID_CREDENTIALS,
		FileErrorCode.STORAGE_SIGNATURE_MISMATCH,
		FileErrorCode.STORAGE_TOKEN_EXPIRED,
		FileErrorCode.STORAGE_REQUEST_EXPIRED,
		FileErrorCode.STORAGE_BUCKET_NOT_FOUND,
		FileErrorCode.STORAGE_OBJECT_NOT_FOUND,
		FileErrorCode.STORAGE_BUCKET_INVALID,
		FileErrorCode.STORAGE_INVALID_REQUEST,
		FileErrorCode.STORAGE_THROTTLED,
		FileErrorCode.STORAGE_SERVICE_UNAVAILABLE,
		FileErrorCode.STORAGE_INTERNAL_ERROR,
		FileErrorCode.STORAGE_ERROR))),
	IMAGE_SUMMARY(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
		CommonErrorCode.EXTERNAL_SERVICE_TIMEOUT,
		FileErrorCode.STORAGE_ACCESS_DENIED,
		FileErrorCode.STORAGE_INVALID_CREDENTIALS,
		FileErrorCode.STORAGE_SIGNATURE_MISMATCH,
		FileErrorCode.STORAGE_TOKEN_EXPIRED,
		FileErrorCode.STORAGE_REQUEST_EXPIRED,
		FileErrorCode.STORAGE_BUCKET_NOT_FOUND,
		FileErrorCode.STORAGE_OBJECT_NOT_FOUND,
		FileErrorCode.STORAGE_BUCKET_INVALID,
		FileErrorCode.STORAGE_INVALID_REQUEST,
		FileErrorCode.STORAGE_THROTTLED,
		FileErrorCode.STORAGE_SERVICE_UNAVAILABLE,
		FileErrorCode.STORAGE_INTERNAL_ERROR,
		FileErrorCode.STORAGE_ERROR)));

	private final Set<ErrorCode> errorCodeList;

	FileSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
