package com.tasteam.infra.storage;

import java.nio.file.Path;
import java.util.List;

public interface StorageClient {

	PresignedPostResponse createPresignedPost(PresignedPostRequest request);

	String createPresignedGetUrl(String objectKey);

	void deleteObject(String objectKey);

	byte[] downloadObject(String objectKey);

	void uploadObject(String objectKey, byte[] data, String contentType);

	void uploadObject(String objectKey, Path file, String contentType);

	List<String> listObjects(String prefix);
}
