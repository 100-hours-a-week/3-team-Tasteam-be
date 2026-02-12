package com.tasteam.domain.admin.dto.response;

import java.time.Instant;

import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.DisplayStatus;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminPromotionListItem(
	@Schema(description = "프로모션 ID", example = "1")
	Long id,

	@Schema(description = "제목", example = "신규 가입 이벤트")
	String title,

	@Schema(description = "프로모션 상태", example = "ONGOING")
	PromotionStatus promotionStatus,

	@Schema(description = "노출 상태", example = "DISPLAYING")
	DisplayStatus displayStatus,

	@Schema(description = "발행 상태", example = "PUBLISHED")
	PublishStatus publishStatus,

	@Schema(description = "프로모션 시작일")
	Instant promotionStartAt,

	@Schema(description = "프로모션 종료일")
	Instant promotionEndAt,

	@Schema(description = "노출 채널", example = "MAIN_BANNER")
	DisplayChannel displayChannel,

	@Schema(description = "배너 이미지 URL")
	String bannerImageUrl,

	@Schema(description = "생성일시")
	Instant createdAt) {
}
