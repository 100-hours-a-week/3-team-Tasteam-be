package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

import com.tasteam.domain.subgroup.entity.Subgroup;

public record SubgroupCreateResponse(SubgroupCreateData data) {

	public static SubgroupCreateResponse from(Subgroup subgroup) {
		return new SubgroupCreateResponse(
			new SubgroupCreateData(
				subgroup.getId(),
				subgroup.getCreatedAt()));
	}

	public record SubgroupCreateData(Long id, Instant createdAt) {
	}
}
