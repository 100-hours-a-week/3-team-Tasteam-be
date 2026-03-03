package com.tasteam.domain.admin.dto.response;

public record AdminDataCountResponse(
	long memberCount,
	long restaurantCount,
	long groupCount,
	long subgroupCount,
	long reviewCount,
	long chatMessageCount) {
}
