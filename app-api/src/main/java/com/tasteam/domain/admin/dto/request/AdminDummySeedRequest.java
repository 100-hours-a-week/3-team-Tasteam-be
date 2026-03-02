package com.tasteam.domain.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminDummySeedRequest(
	@Min(value = 0) @Max(value = 2000)
	int memberCount,
	@Min(value = 0) @Max(value = 2000)
	int restaurantCount,
	@Min(value = 0) @Max(value = 200)
	int groupCount,
	@Min(value = 0) @Max(value = 50)
	int subgroupPerGroup,
	@Min(value = 0) @Max(value = 500)
	int memberPerGroup,
	@Min(value = 0) @Max(value = 10000)
	int reviewCount,
	@Min(value = 0) @Max(value = 200)
	int chatMessagePerRoom) {
}
