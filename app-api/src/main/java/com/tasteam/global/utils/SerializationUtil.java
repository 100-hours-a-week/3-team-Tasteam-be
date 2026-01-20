package com.tasteam.global.utils;

import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 직렬화/역직렬화 헬퍼
 */
public final class SerializationUtil {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
		.findAndRegisterModules()
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private SerializationUtil() {}

	public static String serialize(Object value) {
		try {
			byte[] payload = OBJECT_MAPPER.writeValueAsBytes(value);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
		} catch (Exception e) {
			throw new IllegalStateException("Serialization failed", e);
		}
	}

	public static <T> T deserialize(String serialized, Class<T> targetClass) {
		try {
			byte[] payload = Base64.getUrlDecoder().decode(serialized);
			return OBJECT_MAPPER.readValue(payload, targetClass);
		} catch (Exception e) {
			throw new IllegalStateException("Deserialization failed", e);
		}
	}
}
