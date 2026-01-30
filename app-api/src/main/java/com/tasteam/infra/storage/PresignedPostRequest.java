package com.tasteam.infra.storage;

public record PresignedPostRequest(
	String objectKey,
	String contentType,
	long minContentLength,
	long maxContentLength) {
}
