package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

import com.tasteam.domain.subgroup.entity.Subgroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubgroupCreateResponse {

	private SubgroupCreateData data;

	public static SubgroupCreateResponse from(Subgroup subgroup) {
		return SubgroupCreateResponse.builder()
			.data(SubgroupCreateData.builder()
				.id(subgroup.getId())
				.createdAt(subgroup.getCreatedAt())
				.build())
			.build();
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SubgroupCreateData {
		private Long id;
		private Instant createdAt;
	}
}
