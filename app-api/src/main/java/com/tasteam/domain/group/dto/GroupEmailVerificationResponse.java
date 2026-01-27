package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.tasteam.domain.group.entity.GroupAuthCode;

public record GroupEmailVerificationResponse(
	Long id,
	Instant createdAt,
	Instant expiresAt) {

	public static GroupEmailVerificationResponse from(GroupAuthCode authCode) {
		return new GroupEmailVerificationResponse(
			authCode.getId(),
			authCode.getCreatedAt(),
			authCode.getExpiresAt());
	}
}
