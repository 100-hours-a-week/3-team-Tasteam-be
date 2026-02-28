package com.tasteam.infra.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.email")
public class EmailProperties {

	private String type;
	private String region;
	private String from;
	private String accessKey;
	private String secretKey;

	public boolean hasStaticCredentials() {
		return accessKey != null && !accessKey.isBlank()
			&& secretKey != null && !secretKey.isBlank();
	}

	@PostConstruct
	public void logConfiguration() {
		log.info("=== Email Configuration ===");
		log.info("type      : {}", isConfigured(type) ? type : "(not set)");
		log.info("region    : {}", isConfigured(region) ? region : "(not set)");
		log.info("from      : {}", isConfigured(from) ? from : "(not set)");
		log.info("accessKey : {}", isConfigured(accessKey) ? maskValue(accessKey) : "(not set - DefaultChain 사용)");
		log.info("secretKey : {}", isConfigured(secretKey) ? "****" : "(not set - DefaultChain 사용)");
		log.info("===========================");
	}

	private boolean isConfigured(String value) {
		return value != null && !value.isBlank();
	}

	private String maskValue(String value) {
		if (value == null || value.length() <= 4) {
			return "****";
		}
		return value.substring(0, 4) + "****";
	}
}
