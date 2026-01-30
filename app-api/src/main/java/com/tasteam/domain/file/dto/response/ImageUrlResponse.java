package com.tasteam.domain.file.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUrlResponse(
	@Schema(description = "파일 UUID(외부 식별자)", example = "a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012")
	String fileUuid,
	@Schema(description = "공개 조회 URL", example = "https://cdn.example.com/uploads/temp/a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012.jpg")
	String url) {
}
