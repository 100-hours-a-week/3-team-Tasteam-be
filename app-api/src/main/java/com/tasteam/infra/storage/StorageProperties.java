package com.tasteam.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.storage")
public class StorageProperties {

	private String type;
	private String region;
	private String bucket;
	private String baseUrl;
	private String accessKey;
	private String secretKey;
	private long presignedExpirationSeconds = 300;
	private String tempUploadPrefix = "uploads/temp";

	public boolean hasStaticCredentials() {
		return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
	}
}
