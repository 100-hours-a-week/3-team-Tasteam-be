package com.tasteam.domain.promotion.dto.response;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.promotion.dto.PromotionDetailDto;
import com.tasteam.domain.promotion.entity.PromotionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record PromotionDetailResponse(
	@Schema(description = "이벤트 ID", example = "1")
	Long id,

	@Schema(description = "이벤트 제목", example = "신규 가입 이벤트")
	String title,

	@Schema(description = "이벤트 본문", example = "첫 리뷰 작성 시 특별 쿠폰을 드립니다. 자세한 내용은...")
	String content,

	@Schema(description = "랜딩 URL", example = "https://example.com/event/1")
	String landingUrl,

	@Schema(description = "이벤트 시작 시각")
	Instant promotionStartAt,

	@Schema(description = "이벤트 종료 시각")
	Instant promotionEndAt,

	@Schema(description = "이벤트 상태 (UPCOMING/ONGOING/ENDED)")
	PromotionStatus promotionStatus,

	@Schema(description = "노출 시작 시각")
	Instant displayStartAt,

	@Schema(description = "노출 종료 시각")
	Instant displayEndAt,

	@Schema(description = "배너 이미지 URL", example = "https://example.com/banner.jpg")
	String bannerImageUrl,

	@Schema(description = "상세 이미지 URL 목록")
	List<String> detailImageUrls) {
	public static PromotionDetailResponse fromDto(PromotionDetailDto dto) {
		Instant now = Instant.now();
		PromotionStatus promotionStatus = PromotionStatus.calculate(dto.promotionStartAt(), dto.promotionEndAt(), now);

		return new PromotionDetailResponse(
			dto.promotionId(),
			dto.title(),
			dto.content(),
			dto.landingUrl(),
			dto.promotionStartAt(),
			dto.promotionEndAt(),
			promotionStatus,
			dto.displayStartAt(),
			dto.displayEndAt(),
			dto.bannerImageUrl(),
			dto.detailImageUrls());
	}
}
