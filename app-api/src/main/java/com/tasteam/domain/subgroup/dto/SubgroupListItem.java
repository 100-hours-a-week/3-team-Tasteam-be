package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

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
	private String profileImageUrl;
	private SubgroupJoinType joinType;
	private Instant createdAt;

	// For JPQL constructor projection without joinType.
	public SubgroupListItem(Long subgroupId, String name, String description, Integer memberCount,
		String profileImageUrl, Instant createdAt) {
		this.subgroupId = subgroupId;
		this.name = name;
		this.description = description;
		this.memberCount = memberCount;
		this.profileImageUrl = profileImageUrl;
		this.createdAt = createdAt;
	}
}
