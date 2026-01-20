package com.tasteam.domain.group.service;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GroupErrorCode implements ErrorCode {

	GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
