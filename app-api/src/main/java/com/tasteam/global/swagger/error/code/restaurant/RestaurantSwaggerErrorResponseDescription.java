package com.tasteam.global.swagger.error.code.restaurant;

import java.util.LinkedHashSet;
import java.util.Set;

import com.tasteam.global.exception.ErrorCode;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import lombok.Getter;

@Getter
public enum RestaurantSwaggerErrorResponseDescription implements SwaggerErrorResponseDescription {

	RESTAURANT_LIST_MOCK(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST))),
	RESTAURANT_DETAIL(new LinkedHashSet<>(Set.of(
		RestaurantErrorCode.RESTAURANT_NOT_FOUND))),
	RESTAURANT_CREATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST))),
	RESTAURANT_UPDATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		RestaurantErrorCode.RESTAURANT_NOT_FOUND))),
	RESTAURANT_DELETE(new LinkedHashSet<>(Set.of(
		RestaurantErrorCode.RESTAURANT_NOT_FOUND))),
	RESTAURANT_REVIEWS(new LinkedHashSet<>(Set.of(
		RestaurantErrorCode.RESTAURANT_NOT_FOUND))),
	RESTAURANT_REVIEW_CREATE(new LinkedHashSet<>(Set.of(
		CommonErrorCode.INVALID_REQUEST,
		RestaurantErrorCode.RESTAURANT_NOT_FOUND))),
	MENU_LIST(new LinkedHashSet<>(Set.of(
		RestaurantErrorCode.RESTAURANT_NOT_FOUND))),
	MENU_CATEGORY_CREATE(new LinkedHashSet<>(Set.of(
		RestaurantErrorCode.RESTAURANT_NOT_FOUND))),
	MENU_CREATE(new LinkedHashSet<>(Set.of(
		RestaurantErrorCode.RESTAURANT_NOT_FOUND,
		RestaurantErrorCode.MENU_CATEGORY_NOT_FOUND))),
	MENU_BULK_CREATE(new LinkedHashSet<>(Set.of(
		RestaurantErrorCode.RESTAURANT_NOT_FOUND,
		RestaurantErrorCode.MENU_CATEGORY_NOT_FOUND)));

	private final Set<ErrorCode> errorCodeList;

	RestaurantSwaggerErrorResponseDescription(Set<ErrorCode> errorCodeList) {
		this.errorCodeList = withCommonErrors(errorCodeList);
	}
}
