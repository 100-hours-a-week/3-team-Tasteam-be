package com.tasteam.infra.storage;

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
@ConfigurationProperties(prefix = "tasteam.storage")
public class StorageProperties {

	private String type;
	private String region;
	private String bucket;
	private String baseUrl;
	private String accessMode = "public";
	private String accessKey;
	private String secretKey;
	private long presignedExpirationSeconds = 300;
	private String tempUploadPrefix = "uploads/temp";

	public boolean hasStaticCredentials() {
		return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
	}

	@PostConstruct
	public void logConfiguration() {
		log.info("=== Storage Configuration ===");
		log.info("type              : {}", isConfigured(type) ? type : "(not set)");
		log.info("region            : {}", isConfigured(region) ? region : "(not set)");
		log.info("bucket            : {}", isConfigured(bucket) ? bucket : "(not set)");
		log.info("baseUrl           : {}", isConfigured(baseUrl) ? baseUrl : "(not set)");
		log.info("accessMode        : {} (presigned={})", accessMode, isPresignedAccess());
		log.info("accessKey         : {}",
			isConfigured(accessKey) ? maskValue(accessKey) : "(not set - DefaultChain 사용)");
		log.info("secretKey         : {}", isConfigured(secretKey) ? "****" : "(not set - DefaultChain 사용)");
		log.info("presignedExpirySec: {}", presignedExpirationSeconds);
		log.info("tempUploadPrefix  : {}", tempUploadPrefix);
		log.info("==============================");
	}

	private boolean isConfigured(String value) {
		return value != null && !value.isBlank();
	}

	public boolean isPresignedAccess() {
		return "presigned".equalsIgnoreCase(accessMode);
	}

	private String maskValue(String value) {
		if (value == null || value.length() <= 4) {
			return "****";
		}
		return value.substring(0, 4) + "****";
	}
}
