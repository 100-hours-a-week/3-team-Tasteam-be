package com.tasteam.global.swagger.error.code.member;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum MemberSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	MEMBER_ME(new LinkedHashSet<>(Set.of(
		MemberErrorCode.MEMBER_NOT_FOUND))),
	MEMBER_GROUP_SUMMARIES(new LinkedHashSet<>(Set.of(
		MemberErrorCode.MEMBER_NOT_FOUND))),
	MEMBER_PROFILE_UPDATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		MemberErrorCode.MEMBER_NOT_FOUND,
		MemberErrorCode.EMAIL_ALREADY_EXISTS))),
	MEMBER_WITHDRAW(new LinkedHashSet<>(Set.of(
		MemberErrorCode.MEMBER_NOT_FOUND)));

	private final Set<ErrorCode> errorCodeList;

	MemberSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
