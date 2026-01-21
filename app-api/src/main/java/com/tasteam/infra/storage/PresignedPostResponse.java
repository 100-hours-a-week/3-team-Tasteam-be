package com.tasteam.infra.storage;

import java.time.Instant;
import java.util.Map;

public record PresignedPostResponse(
	String url,
	Map<String, String> fields,
	Instant expiresAt) {
}
