package com.tasteam.domain.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminDummySeedRequest(
	@Min(value = 0) @Max(value = 100000)
	int memberCount,
	@Min(value = 0) @Max(value = 50000000)
	int restaurantCount,
	@Min(value = 0) @Max(value = 1000)
	int groupCount,
	@Min(value = 0) @Max(value = 1000)
	int subgroupPerGroup,
	@Min(value = 0) @Max(value = 1000)
	int memberPerGroup,
	@Min(value = 0) @Max(value = 100000000)
	int reviewCount,
	@Min(value = 0) @Max(value = 1000000000)
	int chatMessagePerRoom) {
}
