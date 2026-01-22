package com.tasteam.domain.restaurant.geocoding;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NaverGeocodingErrorCode implements ErrorCode {

	OK(HttpStatus.OK, "성공"),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, ""),
	SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ""),
	UNKNOWN(HttpStatus.INTERNAL_SERVER_ERROR, "");

	private final HttpStatus httpStatus;
	private final String message;

	public static NaverGeocodingErrorCode from(String value) {
		try {
			return NaverGeocodingErrorCode.valueOf(value);

		} catch (Exception e) {
			return UNKNOWN;
		}
	}
}
