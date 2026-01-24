package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubgroupErrorCode implements ErrorCode {

	SUBGROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "하위그룹을 찾을 수 없습니다"),
	SUBGROUP_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 가입된 하위그룹입니다"),
	PASSWORD_MISMATCH(HttpStatus.CONFLICT, "비밀번호가 일치하지 않습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
