package com.tasteam.domain.auth.dto.request;

import com.tasteam.global.validation.ValidationPatterns;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "로컬 개발용 토큰 발급 요청")
public record LocalAuthTokenRequest(
	@Schema(description = "이메일", example = "test1@dev.local") @NotBlank(message = "email은 필수입니다") @Pattern(regexp = ValidationPatterns.EMAIL_PATTERN, message = "email 형식이 올바르지 않습니다") @Size(max = 255, message = "email 길이는 255자 이하여야 합니다")
	String email,

	@Schema(description = "닉네임", example = "테스트유저") @NotBlank(message = "nickname은 필수입니다") @Size(max = 50, message = "nickname 길이는 50자 이하여야 합니다")
	String nickname) {
}
