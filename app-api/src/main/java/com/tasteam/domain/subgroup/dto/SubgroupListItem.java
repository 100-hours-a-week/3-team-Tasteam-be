package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

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
	private Instant createdAt;
}
