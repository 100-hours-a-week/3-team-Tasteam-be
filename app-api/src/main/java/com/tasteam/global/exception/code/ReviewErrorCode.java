package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

	REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다"),
	KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰 키워드를 찾을 수 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
