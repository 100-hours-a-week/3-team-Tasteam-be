package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.tasteam.domain.group.entity.Group;

public record GroupGetResponse(GroupData data) {

	public static GroupGetResponse of(GroupData data) {
		return new GroupGetResponse(data);
	}

	public static GroupGetResponse from(Group group, String logoImageUrl, long memberCount) {
		return new GroupGetResponse(
			new GroupData(
				group.getId(),
				group.getName(),
				logoImageUrl,
				group.getAddress(),
				group.getDetailAddress(),
				group.getEmailDomain(),
				memberCount,
				group.getStatus().name(),
				group.getCreatedAt(),
				group.getUpdatedAt()));
	}

	public record GroupData(
		Long groupId,
		String name,
		String logoImageUrl,
		String address,
		String detailAddress,
		String emailDomain,
		long memberCount,
		String status,
		Instant createdAt,
		Instant updatedAt) {
	}
}
