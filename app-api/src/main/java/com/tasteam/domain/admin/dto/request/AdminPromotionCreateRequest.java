package com.tasteam.domain.admin.dto.request;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.PublishStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPromotionCreateRequest(
	@NotBlank(message = "제목은 필수입니다")
	String title,

	@NotBlank(message = "내용은 필수입니다")
	String content,

	String landingUrl,

	@NotNull(message = "프로모션 시작일은 필수입니다")
	Instant promotionStartAt,

	@NotNull(message = "프로모션 종료일은 필수입니다")
	Instant promotionEndAt,

	@NotNull(message = "발행 상태는 필수입니다")
	PublishStatus publishStatus,

	@NotNull(message = "노출 활성화 여부는 필수입니다")
	Boolean displayEnabled,

	@NotNull(message = "노출 시작일은 필수입니다")
	Instant displayStartAt,

	@NotNull(message = "노출 종료일은 필수입니다")
	Instant displayEndAt,

	@NotNull(message = "노출 채널은 필수입니다")
	DisplayChannel displayChannel,

	@NotNull(message = "노출 우선순위는 필수입니다")
	Integer displayPriority,

	@NotBlank(message = "배너 이미지 URL은 필수입니다")
	String bannerImageUrl,

	String bannerImageAltText,

	List<String> detailImageUrls) {
}
