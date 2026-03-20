package com.tasteam.infra.messagequeue;

public record SearchCompletedMessagePayload(
	Long memberId,
	String keyword,
	int groupResultCount,
	int restaurantResultCount) {
}
