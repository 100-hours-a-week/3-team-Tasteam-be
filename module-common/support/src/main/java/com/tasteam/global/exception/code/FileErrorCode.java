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
	STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "스토리지 처리 중 오류가 발생했습니다"),
	STORAGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "스토리지 접근 권한이 없습니다"),
	STORAGE_INVALID_CREDENTIALS(HttpStatus.FORBIDDEN, "스토리지 자격 증명이 유효하지 않습니다"),
	STORAGE_SIGNATURE_MISMATCH(HttpStatus.FORBIDDEN, "스토리지 서명 검증에 실패했습니다"),
	STORAGE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "스토리지 인증 토큰이 만료되었습니다"),
	STORAGE_REQUEST_EXPIRED(HttpStatus.BAD_REQUEST, "스토리지 요청 시간이 유효하지 않습니다"),
	STORAGE_BUCKET_NOT_FOUND(HttpStatus.NOT_FOUND, "스토리지 버킷을 찾을 수 없습니다"),
	STORAGE_OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "스토리지 객체를 찾을 수 없습니다"),
	STORAGE_BUCKET_INVALID(HttpStatus.BAD_REQUEST, "스토리지 버킷 설정이 올바르지 않습니다"),
	STORAGE_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "스토리지 요청이 올바르지 않습니다"),
	STORAGE_ENTITY_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "스토리지 업로드 파일이 너무 큽니다"),
	STORAGE_ENTITY_TOO_SMALL(HttpStatus.BAD_REQUEST, "스토리지 업로드 파일이 너무 작습니다"),
	STORAGE_THROTTLED(HttpStatus.TOO_MANY_REQUESTS, "스토리지 요청이 너무 많습니다"),
	STORAGE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "스토리지 서비스가 일시적으로 불안정합니다"),
	STORAGE_INTERNAL_ERROR(HttpStatus.BAD_GATEWAY, "스토리지 내부 오류가 발생했습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
