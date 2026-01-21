package com.tasteam.infra.storage;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;

@Component
@Primary
@Profile("!test")
@ConditionalOnProperty(prefix = "tasteam.storage", name = "type", havingValue = "dummy", matchIfMissing = true)
@RequiredArgsConstructor
public class DummyStorageClient implements StorageClient {

	private final StorageProperties properties;

	@Override
	public PresignedPostResponse createPresignedPost(PresignedPostRequest request) {
		Assert.notNull(request, "presigned 요청은 필수입니다");
		Assert.hasText(request.objectKey(), "objectKey는 필수입니다");
		Assert.hasText(request.contentType(), "contentType은 필수입니다");

		Instant expiresAt = Instant.now().plusSeconds(properties.getPresignedExpirationSeconds());
		String url = buildFallbackUrl(request.objectKey());

		return new PresignedPostResponse(url, Map.of(), expiresAt);
	}

	@Override
	public void deleteObject(String objectKey) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
		// No external storage to clean up.
	}

	private String buildFallbackUrl(String objectKey) {
		String baseUrl = properties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			return objectKey;
		}
		String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
		return normalizedBase + "/" + normalizedKey;
	}
}
