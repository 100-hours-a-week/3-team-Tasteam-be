package com.tasteam.global.validation;

import java.util.List;
import java.util.Locale;

public final class KeywordSecurityPolicy {

	private static final List<String> BLOCKED_TOKENS = List.of(
		"<",
		">",
		"'",
		"\"",
		";",
		"\\",
		"--",
		"/*",
		"*/",
		"javascript:",
		"onerror=",
		"onload=");

	private KeywordSecurityPolicy() {}

	public static boolean isSafeKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return false;
		}

		String normalized = keyword.toLowerCase(Locale.ROOT);
		if (containsControlCharacter(normalized)) {
			return false;
		}

		return BLOCKED_TOKENS.stream()
			.noneMatch(normalized::contains);
	}

	private static boolean containsControlCharacter(String value) {
		return value.chars()
			.anyMatch(Character::isISOControl);
	}
}
