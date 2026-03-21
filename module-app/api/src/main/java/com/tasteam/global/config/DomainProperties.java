package com.tasteam.global.config;

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
@ConfigurationProperties(prefix = "tasteam.domain")
public class DomainProperties {

	private String service;
	private String api;

	@PostConstruct
	public void logConfiguration() {
		log.info("=== Domain Configuration ===");
		log.info("service: {}", isConfigured(service) ? service : "(not set)");
		log.info("api: {}", isConfigured(api) ? api : "(not set)");
		log.info("=============================");
	}

	private boolean isConfigured(String value) {
		return value != null && !value.isBlank();
	}
}
