package com.tasteam.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

		private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

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
		public String createPresignedGetUrl(String objectKey) {
			return "https://fake-storage.local/" + objectKey;
		}

		@Override
		public void deleteObject(String objectKey) {
			objects.remove(objectKey);
		}

		@Override
		public byte[] downloadObject(String objectKey) {
			return objects.getOrDefault(objectKey, new byte[0]);
		}

		@Override
		public void uploadObject(String objectKey, byte[] data, String contentType) {
			objects.put(objectKey, data == null ? new byte[0] : data);
		}

		@Override
		public List<String> listObjects(String prefix) {
			List<String> result = new ArrayList<>();
			for (String key : objects.keySet()) {
				if (key.startsWith(prefix)) {
					result.add(key);
				}
			}
			return result;
		}
	}
}
