package com.tasteam.domain.file.dto.response;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public record PresignedUploadItem(
	@Schema(description = "파일 UUID(외부 식별자)", example = "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012")
	String fileUuid,
	@Schema(description = "S3 Object Key", example = "uploads/temp/a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012.jpg")
	String objectKey,
	@Schema(description = "Presigned POST 업로드 대상 URL", example = "https://my-bucket.s3.ap-northeast-2.amazonaws.com")
	String url,
	@Schema(description = "Presigned POST form fields", type = "object", example = "{\"key\":\"uploads/temp/a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012.jpg\",\"policy\":\"base64-policy\",\"x-amz-algorithm\":\"AWS4-HMAC-SHA256\",\"x-amz-credential\":\"AKIA.../20260130/ap-northeast-2/s3/aws4_request\",\"x-amz-date\":\"20260130T010000Z\",\"x-amz-signature\":\"abc123\",\"Content-Type\":\"image/jpeg\"}")
	Map<String, String> fields,
	@Schema(description = "만료 시각(UTC, ISO-8601)", example = "2026-01-30T01:05:00Z")
	Instant expiresAt) {
}
