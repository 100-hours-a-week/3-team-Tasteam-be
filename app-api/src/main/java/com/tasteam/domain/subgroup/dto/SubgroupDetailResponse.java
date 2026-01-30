package com.tasteam.domain.subgroup.dto;

import java.time.Instant;
import java.util.UUID;

public record SubgroupDetailResponse(SubgroupDetail data) {

	public record SubgroupDetail(
		Long groupId,
		Long subgroupId,
		String name,
		String description,
		Integer memberCount,
		ProfileImage profileImage,
		Instant createdAt) {
	}

	public record ProfileImage(
		UUID id,
		String url) {
	}
}
