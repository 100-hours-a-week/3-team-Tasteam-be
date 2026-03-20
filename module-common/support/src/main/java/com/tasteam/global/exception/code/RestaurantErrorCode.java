package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RestaurantErrorCode implements ErrorCode {

	RESTAURANT_LIST_ENDPOINT_DISABLED(HttpStatus.GONE, "음식점 목록 조회 API는 더 이상 지원하지 않습니다."),
	RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "음식점 정보를 찾을 수 없습니다."),
	FOOD_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "음식 카테고리 정보를 찾을 수 없습니다."),
	MENU_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴 카테고리 정보를 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
