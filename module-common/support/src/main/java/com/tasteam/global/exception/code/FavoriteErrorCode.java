package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FavoriteErrorCode implements ErrorCode {

	FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 찜한 음식점입니다"),
	FAVORITE_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 찜이 해제된 음식점입니다"),
	FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "찜 정보를 찾을 수 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
