package com.tasteam.global.swagger.error.code.subgroup;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.SubgroupErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum SubgroupSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	SUBGROUP_REVIEWS(new LinkedHashSet<>(Set.of(
		SubgroupErrorCode.SUBGROUP_NOT_FOUND))),
	SUBGROUP_LIST_MY(new LinkedHashSet<>(Set.of(
		GroupErrorCode.GROUP_NOT_FOUND))),
	SUBGROUP_LIST_GROUP(new LinkedHashSet<>(Set.of(
		GroupErrorCode.GROUP_NOT_FOUND))),
	SUBGROUP_DETAIL(new LinkedHashSet<>(Set.of(
		SubgroupErrorCode.SUBGROUP_NOT_FOUND))),
	SUBGROUP_CREATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		GroupErrorCode.GROUP_NOT_FOUND))),
	SUBGROUP_JOIN(new LinkedHashSet<>(Set.of(
		SubgroupErrorCode.SUBGROUP_NOT_FOUND,
		SubgroupErrorCode.SUBGROUP_ALREADY_JOINED,
		SubgroupErrorCode.PASSWORD_MISMATCH))),
	SUBGROUP_WITHDRAW(new LinkedHashSet<>(Set.of(
		SubgroupErrorCode.SUBGROUP_NOT_FOUND))),
	SUBGROUP_UPDATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.NO_PERMISSION,
		SubgroupErrorCode.SUBGROUP_NOT_FOUND)));

	private final Set<ErrorCode> errorCodeList;

	SubgroupSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
