package com.tasteam.domain.member.dto.response;

public record ReviewSummaryResponse(
	Long id,
	String restaurantName,
	String restaurantAddress,
	String reviewContent) {
}
