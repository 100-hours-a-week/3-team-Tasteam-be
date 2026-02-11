package com.tasteam.domain.promotion.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tasteam.domain.promotion.dto.SplashPromotionDto;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SplashPromotionResponse(
	@Schema(description = "이벤트 ID", example = "1")
	Long id,

	@Schema(description = "이벤트 제목", example = "신규 가입 이벤트")
	String title,

	@Schema(description = "이벤트 본문", example = "첫 리뷰 작성 시 특별 쿠폰을 드립니다")
	String content,

	@Schema(description = "썸네일 이미지 URL", example = "https://example.com/event.jpg")
	String thumbnailImageUrl,

	@Schema(description = "이벤트 시작 시각")
	Instant startAt,

	@Schema(description = "이벤트 종료 시각")
	Instant endAt) {
	public static SplashPromotionResponse fromDto(SplashPromotionDto dto) {
		return new SplashPromotionResponse(
			dto.promotionId(),
			dto.title(),
			dto.content(),
			dto.thumbnailImageUrl(),
			dto.startAt(),
			dto.endAt());
	}
}
