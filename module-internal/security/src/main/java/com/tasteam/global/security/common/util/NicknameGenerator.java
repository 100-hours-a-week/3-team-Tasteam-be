package com.tasteam.global.security.common.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 간단한 닉네임 자동 생성 유틸.
 */
public final class NicknameGenerator {

	private static final int MAX_LENGTH = 12;
	private static final SecureRandom RANDOM = new SecureRandom();

	private NicknameGenerator() {}

	public static String fromEmail(String email) {
		if (email == null || email.isBlank()) {
			return randomNickname();
		}
		int at = email.indexOf('@');
		String candidate = (at <= 0) ? email : email.substring(0, at);
		return sanitize(candidate);
	}

	public static String fromName(String name) {
		if (name == null || name.isBlank()) {
			return randomNickname();
		}
		return sanitize(name);
	}

	private static String sanitize(String value) {
		String cleaned = value.toLowerCase(Locale.ROOT)
			.trim()
			.replaceAll("[^a-z0-9]", "");
		if (cleaned.isEmpty()) {
			return randomNickname();
		}
		if (cleaned.length() > MAX_LENGTH) {
			return cleaned.substring(0, MAX_LENGTH);
		}
		return cleaned;
	}

	private static String randomNickname() {
		int value = RANDOM.nextInt(9999);
		return "user" + value;
	}
}
