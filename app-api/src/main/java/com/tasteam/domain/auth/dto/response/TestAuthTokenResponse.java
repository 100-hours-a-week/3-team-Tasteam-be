package com.tasteam.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "테스트용 토큰 발급 응답")
public record TestAuthTokenResponse(
	@Schema(description = "액세스 토큰")
	String accessToken,
	@Schema(description = "회원 아이디")
	Long memberId,
	@Schema(description = "신규 생성 여부")
	boolean isNew) {
}
