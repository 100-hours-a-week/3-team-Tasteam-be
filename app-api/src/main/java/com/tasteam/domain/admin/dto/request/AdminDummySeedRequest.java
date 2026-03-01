package com.tasteam.domain.admin.dto.request;

public record AdminDummySeedRequest(
	int memberCount,
	int restaurantCount,
	int groupCount,
	int subgroupPerGroup,
	int memberPerGroup,
	int reviewCount,
	int chatMessagePerRoom) {
}
