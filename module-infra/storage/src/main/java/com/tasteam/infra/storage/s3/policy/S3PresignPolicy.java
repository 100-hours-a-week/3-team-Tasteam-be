package com.tasteam.infra.storage.s3.policy;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;

@Builder
public record S3PresignPolicy(Map<String, String> fields, Instant expiresAt) {
}
