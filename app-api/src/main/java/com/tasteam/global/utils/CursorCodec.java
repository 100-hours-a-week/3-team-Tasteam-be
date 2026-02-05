package com.tasteam.global.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

/**
 * Cursor 인코딩/디코딩 유틸리티
 */
@Component
public class CursorCodec {

	private final ObjectMapper objectMapper;

	public CursorCodec(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * 커서를 인코딩합니다.
	 */
	public String encode(Object cursor) {
		try {
			String json = objectMapper.writeValueAsString(cursor);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));

		} catch (JsonProcessingException e) {
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 커서를 디코딩합니다.
	 */
	public <T> T decode(String cursor, Class<T> type) {
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(cursor);
			return objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), type);

		} catch (IllegalArgumentException | JsonProcessingException e) {
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 커서를 디코딩합니다. 디코딩에 실패하면 {@code null}을 반환합니다.
	 */
	public <T> T decodeOrNull(String cursor, Class<T> type) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			return decode(cursor, type);
		} catch (Exception ex) {
			return null;
		}
	}
}
