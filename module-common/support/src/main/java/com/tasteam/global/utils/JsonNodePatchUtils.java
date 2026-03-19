package com.tasteam.global.utils;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

public final class JsonNodePatchUtils {

	private JsonNodePatchUtils() {}

	public static String applyStringIfPresent(JsonNode node, Consumer<String> updater, boolean nullable,
		boolean rejectBlank) {
		if (node == null) {
			return null;
		}
		if (node.isNull()) {
			if (!nullable) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			updater.accept(null);
			return null;
		}
		if (!node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		String value = node.asText();
		if (rejectBlank && value.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		updater.accept(value);
		return value;
	}
}
