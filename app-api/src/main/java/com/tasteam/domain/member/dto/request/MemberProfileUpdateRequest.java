package com.tasteam.domain.member.dto.request;

import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
	@Pattern(regexp = ValidationPatterns.EMAIL_PATTERN, message = "email 형식이 올바르지 않습니다") @Size(max = 255, message = "email 길이는 255자 이하여야 합니다")
	String email,
	@Size(max = ValidationPatterns.NICKNAME_MAX_LENGTH, message = "nickname 길이는 30자 이하여야 합니다")
	String nickname,
	@Size(max = ValidationPatterns.INTRODUCTION_MAX_LENGTH, message = "introduction 길이는 500자 이하여야 합니다")
	String introduction,
	@Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "profileImageFileUuid 형식이 올바르지 않습니다")
	String profileImageFileUuid) {
}
