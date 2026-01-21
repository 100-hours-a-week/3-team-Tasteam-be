package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

	FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일 정보를 찾을 수 없습니다"),
	FILE_CONFLICT(HttpStatus.CONFLICT, "이미 도메인에 연결된 파일입니다"),
	FILE_NOT_ACTIVE(HttpStatus.CONFLICT, "파일 상태가 유효하지 않습니다"),
	STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "스토리지 처리 중 오류가 발생했습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
