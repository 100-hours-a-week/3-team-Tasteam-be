package com.tasteam.infra.storage;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresignedUrlCacheService {

	private final StorageClient storageClient;

	@Cacheable(cacheNames = "presigned-url", key = "#storageKey")
	public String getPresignedUrl(String storageKey) {
		return storageClient.createPresignedGetUrl(storageKey);
	}
}
