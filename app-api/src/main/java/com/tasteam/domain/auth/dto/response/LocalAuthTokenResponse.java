package com.tasteam.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로컬 개발용 토큰 발급 응답")
public record LocalAuthTokenResponse(
	@Schema(description = "액세스 토큰")
	String accessToken,
	@Schema(description = "회원 아이디")
	Long memberId) {
}
