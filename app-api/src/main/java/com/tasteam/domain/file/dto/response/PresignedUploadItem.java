package com.tasteam.domain.file.dto.response;

import java.time.Instant;
import java.util.Map;

public record PresignedUploadItem(
	String fileUuid,
	String objectKey,
	String url,
	Map<String, String> fields,
	Instant expiresAt) {
}
