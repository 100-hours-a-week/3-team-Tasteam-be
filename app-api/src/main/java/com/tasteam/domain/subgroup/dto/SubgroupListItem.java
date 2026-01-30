package com.tasteam.domain.subgroup.dto;

import java.time.Instant;
import java.util.UUID;

import com.tasteam.domain.subgroup.type.SubgroupJoinType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubgroupListItem {

	private Long subgroupId;
	private String name;
	private String description;
	private Integer memberCount;
	private ProfileImage profileImage;
	private SubgroupJoinType joinType;
	private Instant createdAt;

	// For JPQL constructor projection without joinType.
	public SubgroupListItem(Long subgroupId, String name, String description, Integer memberCount,
		UUID profileImageId, String profileImageUrl, Instant createdAt) {
		this.subgroupId = subgroupId;
		this.name = name;
		this.description = description;
		this.memberCount = memberCount;
		this.profileImage = toProfileImage(profileImageId, profileImageUrl);
		this.createdAt = createdAt;
	}

	// JPQL constructor projection with joinType.
	public SubgroupListItem(Long subgroupId, String name, String description, Integer memberCount,
		UUID profileImageId, String profileImageUrl, SubgroupJoinType joinType, Instant createdAt) {
		this.subgroupId = subgroupId;
		this.name = name;
		this.description = description;
		this.memberCount = memberCount;
		this.profileImage = toProfileImage(profileImageId, profileImageUrl);
		this.joinType = joinType;
		this.createdAt = createdAt;
	}

	private ProfileImage toProfileImage(UUID profileImageId, String profileImageUrl) {
		if (profileImageId == null || profileImageUrl == null) {
			return null;
		}
		return new ProfileImage(profileImageId, profileImageUrl);
	}

	public record ProfileImage(
		UUID id,
		String url) {
	}
}
