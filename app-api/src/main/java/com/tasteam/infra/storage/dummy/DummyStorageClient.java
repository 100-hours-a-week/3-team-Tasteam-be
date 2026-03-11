package com.tasteam.infra.storage.dummy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.tasteam.infra.storage.PresignedPostRequest;
import com.tasteam.infra.storage.PresignedPostResponse;
import com.tasteam.infra.storage.StorageClient;
import com.tasteam.infra.storage.StorageProperties;

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
		Assert.isTrue(request.minContentLength() > 0, "minContentLength는 1 이상이어야 합니다");
		Assert.isTrue(request.maxContentLength() >= request.minContentLength(),
			"maxContentLength는 minContentLength 이상이어야 합니다");

		Instant expiresAt = Instant.now().plusSeconds(properties.getPresignedExpirationSeconds());
		String url = buildFallbackUrl(request.objectKey());

		return new PresignedPostResponse(url, Map.of(), expiresAt);
	}

	@Override
	public String createPresignedGetUrl(String objectKey) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
		return buildFallbackUrl(objectKey);
	}

	@Override
	public void deleteObject(String objectKey) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
	}

	@Override
	public byte[] downloadObject(String objectKey) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
		return new byte[0];
	}

	@Override
	public void uploadObject(String objectKey, byte[] data, String contentType) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
		Assert.notNull(data, "data는 필수입니다");
		Assert.hasText(contentType, "contentType은 필수입니다");
	}

	@Override
	public void uploadObject(String objectKey, Path file, String contentType) {
		Assert.hasText(objectKey, "objectKey는 필수입니다");
		Assert.notNull(file, "file은 필수입니다");
		Assert.hasText(contentType, "contentType은 필수입니다");
	}

	@Override
	public List<String> listObjects(String prefix) {
		return Collections.emptyList();
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
