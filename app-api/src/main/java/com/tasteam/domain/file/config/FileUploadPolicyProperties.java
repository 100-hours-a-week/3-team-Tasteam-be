package com.tasteam.domain.file.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.file.upload")
public class FileUploadPolicyProperties {

	private long minSizeBytes = 1;
	private long maxSizeBytes = 10485760;
	private List<String> allowedContentTypes = List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

	public boolean isAllowedContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return false;
		}
		String normalized = contentType.trim().toLowerCase(Locale.ROOT);
		return allowedContentTypes.stream()
			.map(value -> value.trim().toLowerCase(Locale.ROOT))
			.anyMatch(normalized::equals);
	}
}
