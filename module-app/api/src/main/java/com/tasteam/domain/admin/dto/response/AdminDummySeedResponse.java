package com.tasteam.domain.admin.dto.response;

public record AdminDummySeedResponse(
	long membersInserted,
	long restaurantsInserted,
	long groupsInserted,
	long subgroupsInserted,
	long reviewsInserted,
	long chatMessagesInserted,
	long notificationsInserted,
	long favoritesInserted,
	long notificationPreferencesInserted,
	long menusInserted,
	long subgroupFavoritesInserted,
	long searchHistoriesInserted,
	long elapsedMs) {
}
