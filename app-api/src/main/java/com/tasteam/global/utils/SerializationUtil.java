package com.tasteam.global.utils;

import java.util.Base64;

import org.springframework.util.SerializationUtils;

/**
 * 직렬화/역직렬화 헬퍼
 */
public final class SerializationUtil {

	private SerializationUtil() {}

	public static String serialize(Object value) {
		try {
			byte[] payload = SerializationUtils.serialize(value);
			if (payload == null) {
				throw new IllegalStateException("Serialization failed");
			}
			return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
		} catch (Exception e) {
			throw new IllegalStateException("Serialization failed", e);
		}
	}

	public static <T> T deserialize(String serialized, Class<T> targetClass) {
		try {
			byte[] payload = Base64.getUrlDecoder().decode(serialized);
			Object value = SerializationUtils.deserialize(payload);
			if (value == null) {
				throw new IllegalStateException("Deserialization failed");
			}
			return targetClass.cast(value);
		} catch (Exception e) {
			throw new IllegalStateException("Deserialization failed", e);
		}
	}
}
