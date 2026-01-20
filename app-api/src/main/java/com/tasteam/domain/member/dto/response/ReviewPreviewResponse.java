package com.tasteam.domain.member.dto.response;

public record ReviewPreviewResponse(
	Long id,
	String restaurantName,
	String restaurantAddress,
	String reviewContent) {
}
