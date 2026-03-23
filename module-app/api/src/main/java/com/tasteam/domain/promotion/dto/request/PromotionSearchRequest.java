package com.tasteam.domain.promotion.dto.request;

import com.tasteam.domain.promotion.entity.PromotionStatus;

import io.swagger.v3.oas.annotations.Parameter;

public record PromotionSearchRequest(
	@Parameter(description = "이벤트 상태 필터 (UPCOMING/ONGOING/ENDED)")
	PromotionStatus promotionStatus) {
}
