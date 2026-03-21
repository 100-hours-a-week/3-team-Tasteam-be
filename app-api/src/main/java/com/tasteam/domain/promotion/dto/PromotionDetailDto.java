package com.tasteam.domain.promotion.dto;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.PublishStatus;

public record PromotionDetailDto(
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
	String bannerImageUrl,
	List<String> detailImageUrls) {
}
