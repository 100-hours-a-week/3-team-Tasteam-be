package com.tasteam.domain.admin.dto.request;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.PublishStatus;

public record AdminPromotionUpdateRequest(
	String title,
	String content,
	String landingUrl,
	Instant promotionStartAt,
	Instant promotionEndAt,
	PublishStatus publishStatus,
	Boolean displayEnabled,
	Instant displayStartAt,
	Instant displayEndAt,
	DisplayChannel displayChannel,
	Integer displayPriority,
	String bannerImageUrl,
	String bannerImageAltText,
	List<String> detailImageUrls) {
}
