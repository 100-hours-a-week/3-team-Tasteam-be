package com.tasteam.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신 응답 DTO")
public record RefreshTokenResponse(
	@Schema(description = "새로운 액세스 토큰")
	String accessToken) {
}
