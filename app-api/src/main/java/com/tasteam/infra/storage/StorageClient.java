package com.tasteam.infra.storage;

public interface StorageClient {

	PresignedPostResponse createPresignedPost(PresignedPostRequest request);

	String createPresignedGetUrl(String objectKey);

	void deleteObject(String objectKey);
}
