package com.tasteam.domain.promotion.dto;

import java.time.Instant;

import com.querydsl.core.annotations.QueryProjection;

public record SplashPromotionDto(
	Long promotionId,
	String title,
	String content,
	String thumbnailImageUrl,
	Instant startAt,
	Instant endAt) {
	@QueryProjection
	public SplashPromotionDto {
	}
}
