package com.tasteam.global.swagger.error.code.group;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum GroupSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	GROUP_CREATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		GroupErrorCode.ALREADY_EXISTS))),
	GROUP_GET(new LinkedHashSet<>(Set.of(
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_UPDATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.NO_PERMISSION,
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_DELETE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.NO_PERMISSION,
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_WITHDRAW(new LinkedHashSet<>(Set.of(
		CommonErrorCode.NO_PERMISSION,
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_EMAIL_VERIFICATION(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_EMAIL_AUTHENTICATION(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		GroupErrorCode.GROUP_NOT_FOUND,
		GroupErrorCode.EMAIL_TOKEN_INVALID,
		GroupErrorCode.EMAIL_TOKEN_EXPIRED))),
	GROUP_PASSWORD_AUTHENTICATION(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		GroupErrorCode.GROUP_NOT_FOUND,
		GroupErrorCode.GROUP_PASSWORD_MISMATCH))),
	GROUP_MEMBERS(new LinkedHashSet<>(Set.of(
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_MEMBER_DELETE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.NO_PERMISSION,
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_REVIEWS(new LinkedHashSet<>(Set.of(
		GroupErrorCode.GROUP_NOT_FOUND))),
	GROUP_REVIEW_RESTAURANTS(new LinkedHashSet<>(Set.of(
		GroupErrorCode.GROUP_NOT_FOUND)));

	private final Set<ErrorCode> errorCodeList;

	GroupSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
