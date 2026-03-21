package com.tasteam.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "테스트용 토큰 발급 요청")
public record TestAuthTokenRequest(
	@Schema(description = "테스트 유저 식별자", example = "test-user-001") @NotBlank(message = "identifier는 필수입니다") @Size(max = 255, message = "identifier 길이는 255자 이하여야 합니다")
	String identifier,

	@Schema(description = "신규 생성 시 사용할 닉네임 (생략 시 랜덤)", example = "부하테스트계정1") @Size(max = 50, message = "nickname 길이는 50자 이하여야 합니다")
	String nickname) {
}
