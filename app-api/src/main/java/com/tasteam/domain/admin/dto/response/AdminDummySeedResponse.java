package com.tasteam.domain.admin.dto.response;

public record AdminDummySeedResponse(
	int membersInserted,
	int restaurantsInserted,
	int groupsInserted,
	int subgroupsInserted,
	int reviewsInserted,
	int chatMessagesInserted,
	long elapsedMs) {
}
