package com.tasteam.infra.storage;

import java.nio.file.Path;
import java.util.List;

public interface StorageClient {

	PresignedPostResponse createPresignedPost(PresignedPostRequest request);

	String createPresignedGetUrl(String objectKey);

	void deleteObject(String objectKey);

	byte[] downloadObject(String objectKey);

	default byte[] downloadObject(String bucket, String objectKey) {
		return downloadObject(objectKey);
	}

	void uploadObject(String objectKey, byte[] data, String contentType);

	void uploadObject(String objectKey, Path file, String contentType);

	List<String> listObjects(String prefix);

	default void deleteObject(String bucket, String objectKey) {
		deleteObject(objectKey);
	}

	default void uploadObject(String bucket, String objectKey, byte[] data, String contentType) {
		uploadObject(objectKey, data, contentType);
	}

	default void uploadObject(String bucket, String objectKey, Path file, String contentType) {
		uploadObject(objectKey, file, contentType);
	}

	default List<String> listObjects(String bucket, String prefix) {
		return listObjects(prefix);
	}
}
