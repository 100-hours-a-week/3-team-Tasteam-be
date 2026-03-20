package com.tasteam.global.utils;

import java.util.Base64;

import org.springframework.util.SerializationUtils;

/**
 * 직렬화/역직렬화 헬퍼
 */
public final class SerializationUtil {

	private SerializationUtil() {}

	/**
	 * 객체를 직렬화합니다.
	 */
	public static String serialize(Object value) {
		try {
			byte[] payload = SerializationUtils.serialize(value);
			if (payload == null) {
				throw new IllegalStateException("직렬화에 실패했습니다");
			}
			return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
		} catch (Exception e) {
			throw new IllegalStateException("직렬화에 실패했습니다", e);
		}
	}

	/**
	 * 직렬화된 문자열을 역직렬화합니다.
	 */
	public static <T> T deserialize(String serialized, Class<T> targetClass) {
		try {
			byte[] payload = Base64.getUrlDecoder().decode(serialized);
			Object value = SerializationUtils.deserialize(payload);
			if (value == null) {
				throw new IllegalStateException("역직렬화에 실패했습니다");
			}
			return targetClass.cast(value);
		} catch (Exception e) {
			throw new IllegalStateException("역직렬화에 실패했습니다", e);
		}
	}
}
