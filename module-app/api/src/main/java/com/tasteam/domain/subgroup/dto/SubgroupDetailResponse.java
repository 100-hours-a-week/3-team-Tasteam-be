package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

public record SubgroupDetailResponse(SubgroupDetail data) {

	public record SubgroupDetail(
		Long groupId,
		Long subgroupId,
		String name,
		String description,
		Integer memberCount,
		String profileImageUrl,
		Instant createdAt) {
	}
}
