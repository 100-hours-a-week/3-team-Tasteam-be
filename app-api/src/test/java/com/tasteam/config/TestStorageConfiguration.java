package com.tasteam.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.tasteam.infra.storage.PresignedPostRequest;
import com.tasteam.infra.storage.PresignedPostResponse;
import com.tasteam.infra.storage.StorageClient;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class TestStorageConfiguration {

	@Bean
	StorageClient storageClient() {
		return new FakeStorageClient();
	}

	private static final class FakeStorageClient implements StorageClient {

		@Override
		public PresignedPostResponse createPresignedPost(PresignedPostRequest request) {
			Map<String, String> fields = new LinkedHashMap<>();
			fields.put("key", request.objectKey());
			fields.put("Content-Type", request.contentType());
			fields.put("x-amz-algorithm", "FAKE");
			fields.put("x-amz-date", Instant.now().toString());
			return new PresignedPostResponse("https://fake-storage.local", Map.copyOf(fields),
				Instant.now().plusSeconds(300));
		}

		@Override
		public void deleteObject(String objectKey) {}
	}
}
