package com.tasteam.domain.search.event;

public record SearchCompletedEvent(
	Long memberId,
	String keyword,
	int groupResultCount,
	int restaurantResultCount) {
}
