package com.tasteam.infra.storage;

public interface StorageClient {

	PresignedPostResponse createPresignedPost(PresignedPostRequest request);

	void deleteObject(String objectKey);
}
