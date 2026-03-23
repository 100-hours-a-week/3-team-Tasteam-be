package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {

	INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "검색 키워드에 허용되지 않는 문자열이 포함되어 있습니다"),
	RECENT_SEARCH_NOT_FOUND(HttpStatus.NOT_FOUND, "최근 검색어가 존재하지 않습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
