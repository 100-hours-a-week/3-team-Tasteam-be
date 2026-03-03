package com.tasteam.domain.admin.dto.response;

public record AdminDummySeedResponse(
	long membersInserted,
	long restaurantsInserted,
	long groupsInserted,
	long subgroupsInserted,
	long reviewsInserted,
	long chatMessagesInserted,
	long elapsedMs) {
}
