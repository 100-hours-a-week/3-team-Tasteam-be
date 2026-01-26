package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.tasteam.domain.group.entity.Group;

public record GroupGetResponse(GroupData data) {

	public static GroupGetResponse from(Group group) {
		return new GroupGetResponse(GroupData.from(group));
	}

	public record GroupData(
		Long groupId,
		String name,
		String logoImageUrl,
		String address,
		String detailAddress,
		String emailDomain,
		String status,
		Instant createdAt,
		Instant updatedAt) {

		private static GroupData from(Group group) {
			return new GroupData(
				group.getId(),
				group.getName(),
				group.getLogoImageUrl(),
				group.getAddress(),
				group.getDetailAddress(),
				group.getEmailDomain(),
				"ACTIVE",
				group.getCreatedAt(),
				group.getUpdatedAt());
		}
	}
}
