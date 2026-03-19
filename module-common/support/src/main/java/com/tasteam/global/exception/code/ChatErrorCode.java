package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

	CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
