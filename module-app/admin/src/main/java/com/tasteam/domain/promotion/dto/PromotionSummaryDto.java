package com.tasteam.domain.promotion.dto;

import java.time.Instant;

import com.querydsl.core.annotations.QueryProjection;
import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.PublishStatus;

public record PromotionSummaryDto(
	Long promotionId,
	String title,
	String content,
	String landingUrl,
	Instant promotionStartAt,
	Instant promotionEndAt,
	PublishStatus publishStatus,
	boolean displayEnabled,
	Instant displayStartAt,
	Instant displayEndAt,
	DisplayChannel displayChannel,
	String bannerImageUrl) {
	@QueryProjection
	public PromotionSummaryDto {
	}
}
