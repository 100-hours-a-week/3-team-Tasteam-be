package com.tasteam.global.swagger.error.code.notification;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.NotificationErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum NotificationSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	NOTIFICATION_LIST(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED))),
	NOTIFICATION_READ(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED,
		NotificationErrorCode.NOTIFICATION_NOT_FOUND))),
	NOTIFICATION_READ_ALL(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED))),
	NOTIFICATION_UNREAD_COUNT(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED))),
	NOTIFICATION_PREFERENCE_LIST(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED))),
	NOTIFICATION_PREFERENCE_UPDATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED,
		CommonErrorCode.INVALID_REQUEST))),
	PUSH_TARGET_REGISTER(new LinkedHashSet<>(Set.of(
		CommonErrorCode.AUTHENTICATION_REQUIRED,
		CommonErrorCode.INVALID_REQUEST)));

	private final Set<ErrorCode> errorCodeList;

	NotificationSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
