package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubgroupErrorCode implements ErrorCode {

	SUBGROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "하위 그룹을 찾을 수 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
