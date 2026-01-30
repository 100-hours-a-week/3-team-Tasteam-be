package com.tasteam.domain.member.dto.request;

import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
	@Pattern(regexp = ValidationPatterns.EMAIL_PATTERN, message = "email 형식이 올바르지 않습니다") @Size(max = 255, message = "email 길이는 255자 이하여야 합니다")
	String email,
	@Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "profileImageId 형식이 올바르지 않습니다")
	String profileImageId) {
}
