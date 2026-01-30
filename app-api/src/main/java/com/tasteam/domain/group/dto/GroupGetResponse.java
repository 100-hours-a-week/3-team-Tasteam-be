package com.tasteam.domain.group.dto;

import java.time.Instant;

public record GroupGetResponse(GroupData data) {

	public static GroupGetResponse of(GroupData data) {
		return new GroupGetResponse(data);
	}

	public record GroupData(
		Long groupId,
		String name,
		LogoImage logoImage,
		String address,
		String detailAddress,
		String emailDomain,
		long memberCount,
		String status,
		Instant createdAt,
		Instant updatedAt) {
	}

	public record LogoImage(
		java.util.UUID id,
		String url) {
	}
}
