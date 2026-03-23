package com.tasteam.domain.admin.dto.request;

public record AdminRecommendationImportRequest(
	String modelVersion,
	@jakarta.validation.constraints.NotBlank(message = "s3PrefixOrUri는 필수입니다")
	String s3PrefixOrUri,
	String requestId) {
}
