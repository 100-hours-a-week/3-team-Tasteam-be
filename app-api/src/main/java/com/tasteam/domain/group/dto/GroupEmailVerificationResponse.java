package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.tasteam.domain.group.entity.GroupAuthCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEmailVerificationResponse {

	private Long id;
	private Instant createdAt;
	private Instant expiresAt;

	public static GroupEmailVerificationResponse from(GroupAuthCode authCode) {
		return GroupEmailVerificationResponse.builder()
			.id(authCode.getId())
			.createdAt(authCode.getCreatedAt())
			.expiresAt(authCode.getExpiresAt())
			.build();
	}
}
