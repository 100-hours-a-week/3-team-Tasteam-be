package com.tasteam.domain.admin.dto.response;

public record AdminDataCountResponse(
	long memberCount,
	long restaurantCount,
	long groupCount,
	long subgroupCount,
	long chatRoomCount,
	long subgroupMemberCount,
	long groupMemberCount,
	long chatRoomMemberCount,
	long reviewCount,
	long chatMessageCount,
	long notificationCount,
	long favoriteCount,
	long subgroupFavoriteCount,
	long memberNotificationPreferenceCount,
	long memberSearchHistoryCount,
	long restaurantAddressCount,
	long restaurantWeeklyScheduleCount,
	long restaurantFoodCategoryCount,
	long menuCategoryCount,
	long menuCount,
	long reviewKeywordCount,
	long restaurantReviewSentimentCount,
	long restaurantReviewSummaryCount) {
}
