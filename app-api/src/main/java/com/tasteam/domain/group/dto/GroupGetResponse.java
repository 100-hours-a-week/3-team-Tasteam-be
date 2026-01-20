package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.tasteam.domain.group.repository.Group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupGetResponse {

	private GroupData data;

	public static GroupGetResponse from(Group group) {
		return GroupGetResponse.builder()
			.data(GroupData.from(group))
			.build();
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class GroupData {
		private Long groupId;
		private String name;
		private String logoImageUrl;
		private String address;
		private String detailAddress;
		private String emailDomain;
		private String status;
		private Instant createdAt;
		private Instant updatedAt;

		private static GroupData from(Group group) {
			return GroupData.builder()
				.groupId(group.getId())
				.name(group.getName())
				.logoImageUrl(group.getLogoImageUrl())
				.address(group.getAddress())
				.detailAddress(group.getDetailAddress())
				.emailDomain(group.getEmailDomain())
				.status("ACTIVE")
				.createdAt(group.getCreatedAt())
				.updatedAt(group.getUpdatedAt())
				.build();
		}
	}
}
