package com.tasteam.global.diagnostics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DB 환경변수/프로퍼티 바인딩 상태를 값 노출 없이 점검한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseEnvPresenceLogger {

	private static final String[] ENV_KEYS = {
		"DB_URL",
		"DB_USERNAME",
		"DB_PASSWORD",
		"FLYWAY_URL",
		"FLYWAY_USER",
		"FLYWAY_PASSWORD"
	};

	private static final String[] PROPERTY_KEYS = {
		"spring.datasource.url",
		"spring.datasource.username",
		"spring.datasource.password",
		"spring.flyway.url",
		"spring.flyway.user",
		"spring.flyway.password"
	};

	private final Environment environment;

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		log.info("=== DB Configuration Injection Diagnostics Start ===");
		for (int i = 0; i < ENV_KEYS.length; i++) {
			logPresence(ENV_KEYS[i], PROPERTY_KEYS[i]);
		}
		log.info("=== DB Configuration Injection Diagnostics End ===");
	}

	private void logPresence(String envKey, String propertyKey) {
		String envValue = environment.getProperty(envKey);
		String propertyValue = environment.getProperty(propertyKey);

		boolean envPresent = hasText(envValue);
		boolean propertyPresent = hasText(propertyValue);

		log.info(
			"db-config probe. envKey={}, envPresent={}, envLen={}, propertyKey={}, propertyPresent={}, propertyLen={}, propertyHash={}",
			envKey,
			envPresent,
			safeLength(envValue),
			propertyKey,
			propertyPresent,
			safeLength(propertyValue),
			safeHash(propertyValue));

		if (!envPresent && !propertyPresent) {
			log.warn("db-config probe warning. {} and {} are not resolved", envKey, propertyKey);
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private int safeLength(String value) {
		return value == null ? 0 : value.length();
	}

	private String safeHash(String value) {
		if (!hasText(value)) {
			return "-";
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hashed.length * 2);
			for (byte b : hashed) {
				hex.append(String.format("%02x", b));
			}
			return hex.substring(0, 12);
		} catch (NoSuchAlgorithmException e) {
			return "hash-error";
		}
	}
}
