package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromotionErrorCode implements ErrorCode {

	PROMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, "프로모션를 찾을 수 없습니다"),
	INVALID_PROMOTION_PERIOD(HttpStatus.BAD_REQUEST, "프로모션 기간이 올바르지 않습니다"),
	INVALID_DISPLAY_PERIOD(HttpStatus.BAD_REQUEST, "노출 기간이 올바르지 않습니다"),
	PROMOTION_DISPLAY_POLICY_MISSING(HttpStatus.BAD_REQUEST, "노출 정책이 없습니다"),
	PROMOTION_BANNER_ASSET_REQUIRED(HttpStatus.BAD_REQUEST, "배너 이미지가 필요합니다"),
	INVALID_PROMOTION_STATUS_FILTER(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 필터입니다"),

	ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항를 찾을 수 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
