package com.tasteam.global.security.common.util;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.global.security.config.properties.CorsProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 리다이렉트 URI의 유효성을 검증하는 유틸리티 클래스
 */
@Slf4j
@Component
public class RedirectUriValidator {

	private final List<String> allowedOrigins;

	public RedirectUriValidator(CorsProperties corsProperties) {
		this.allowedOrigins = corsProperties.getAllowedOrigins();
	}

	public boolean isValidRedirectUri(String redirectUri) {
		if (redirectUri == null || redirectUri.isBlank()) {
			return false;
		}

		try {
			URI uri = URI.create(redirectUri);
			String origin = uri.getScheme() + "://" + uri.getHost() +
				(uri.getPort() != -1 ? ":" + uri.getPort() : "");

			boolean valid = allowedOrigins.contains(origin);

			if (!valid) {
				log.warn("허용되지 않은 리다이렉트 URI 거부: {}", redirectUri);
			}

			return valid;
		} catch (Exception e) {
			log.warn("리다이렉트 URI 형식이 올바르지 않습니다: {}", redirectUri, e);
			return false;
		}
	}
}
