package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {

	RECENT_SEARCH_NOT_FOUND(HttpStatus.NOT_FOUND, "최근 검색어가 존재하지 않습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
