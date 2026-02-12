package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.DisplayStatus;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminPromotionDetailResponse(
	@Schema(description = "프로모션 ID", example = "1")
	Long id,

	@Schema(description = "제목", example = "신규 가입 이벤트")
	String title,

	@Schema(description = "내용", example = "첫 리뷰 작성 시 특별 쿠폰을 드립니다")
	String content,

	@Schema(description = "랜딩 URL", example = "https://example.com/event/1")
	String landingUrl,

	@Schema(description = "프로모션 시작일")
	Instant promotionStartAt,

	@Schema(description = "프로모션 종료일")
	Instant promotionEndAt,

	@Schema(description = "발행 상태", example = "PUBLISHED")
	PublishStatus publishStatus,

	@Schema(description = "프로모션 상태", example = "ONGOING")
	PromotionStatus promotionStatus,

	@Schema(description = "노출 활성화 여부", example = "true")
	Boolean displayEnabled,

	@Schema(description = "노출 시작일")
	Instant displayStartAt,

	@Schema(description = "노출 종료일")
	Instant displayEndAt,

	@Schema(description = "노출 채널", example = "MAIN_BANNER")
	DisplayChannel displayChannel,

	@Schema(description = "노출 우선순위", example = "1")
	Integer displayPriority,

	@Schema(description = "노출 상태", example = "DISPLAYING")
	DisplayStatus displayStatus,

	@Schema(description = "배너 이미지 URL")
	String bannerImageUrl,

	@Schema(description = "배너 이미지 대체 텍스트")
	String bannerImageAltText,

	@Schema(description = "상세 이미지 URL 목록")
	List<String> detailImageUrls,

	@Schema(description = "생성일시")
	Instant createdAt,

	@Schema(description = "수정일시")
	Instant updatedAt) {
}
