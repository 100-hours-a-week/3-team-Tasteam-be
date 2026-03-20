package com.tasteam.global.security.config.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

/**
 * CORS 설정을 외부 설정 파일(application.yml)에서 주입받는 Properties 클래스
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "spring.security.cors")
public class CorsProperties {

	/**
	 * 허용할 Origin 목록 (필수)
	 * 예: ["http://localhost:3000", "https://example.com"]
	 */
	@NotEmpty
	private List<String> allowedOrigins = new ArrayList<>();

	/**
	 * 허용할 HTTP 메서드 목록 (필수)
	 * 예: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
	 */
	@NotEmpty
	private List<String> allowedMethods = new ArrayList<>();

	/**
	 * 허용할 헤더 목록 (없으면 "*" 기본값 사용)
	 * 예: ["*"] 또는 ["Content-Type", "Authorization"]
	 */
	private List<String> allowedHeaders = List.of("*");

	/**
	 * 자격 증명(쿠키, 인증 헤더 등) 포함 허용 여부 (기본값 true)
	 */
	private Boolean allowCredentials = true;

	/**
	 * preflight 요청 캐시 시간(초) (기본값 3600)
	 */
	private Long maxAge = 3600L;

	/**
	 * 클라이언트에 노출할 헤더 목록 (없으면 빈 목록)
	 * 예: ["Set-Cookie", "Authorization"]
	 */
	private List<String> exposedHeaders = new ArrayList<>();
}
