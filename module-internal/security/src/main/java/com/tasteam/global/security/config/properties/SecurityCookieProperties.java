package com.tasteam.global.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.security.cookie")
public class SecurityCookieProperties {
	/**
	 * HTTPS 환경에서는 true 권장.
	 * SameSite=None을 사용할 경우 필수로 true여야 함.
	 */
	private boolean secure = false;

	/**
	 * OAuth2 인가요청/redirect 쿠키 SameSite 정책.
	 * OAuth2 콜백은 "외부 도메인 -> 우리 도메인" top-level GET 리다이렉트로 돌아오는 경우가 많아,
	 * Strict는 쿠키 미전송으로 플로우가 깨질 수 있어 기본값을 Lax로 둔다.
	 *
	 * 허용 값 예: Strict, Lax, None
	 */
	private String oauth2SameSite = "Lax";

	@PostConstruct
	void validate() {
		if ("None".equalsIgnoreCase(oauth2SameSite) && !secure) {
			throw new IllegalStateException(
				"spring.security.cookie.oauth2-same-site=None requires spring.security.cookie.secure=true");
		}
	}
}
