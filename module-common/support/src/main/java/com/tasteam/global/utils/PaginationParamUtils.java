package com.tasteam.global.utils;

import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

public final class PaginationParamUtils {

	private PaginationParamUtils() {}

	public static int resolveSize(Integer size) {
		if (size == null) {
			return 10;
		}
		if (size < 1 || size > 100) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return size;
	}

	public static Long parseLongCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(cursor);
		} catch (NumberFormatException ex) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
