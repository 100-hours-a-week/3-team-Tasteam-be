package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"),
	NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 접근 권한이 없습니다"),
	INVALID_FCM_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 FCM 토큰입니다"),
	INVALID_NOTIFICATION_PREFERENCE(HttpStatus.BAD_REQUEST, "유효하지 않은 알림 선호도 설정입니다"),
	EMAIL_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
	EMAIL_BLOCKED_24H(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많아 24시간 동안 차단되었습니다."),
	EMAIL_RATE_LIMITER_UNAVAILABLE(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요.");

	private final HttpStatus httpStatus;
	private final String message;
}
