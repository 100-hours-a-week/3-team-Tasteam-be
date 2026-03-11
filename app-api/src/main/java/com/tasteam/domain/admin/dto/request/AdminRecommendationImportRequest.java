package com.tasteam.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRecommendationImportRequest(
	@NotBlank(message = "modelVersion은 필수입니다")
	String modelVersion,
	@NotBlank(message = "s3PrefixOrUri는 필수입니다")
	String s3PrefixOrUri,
	String requestId) {
}
