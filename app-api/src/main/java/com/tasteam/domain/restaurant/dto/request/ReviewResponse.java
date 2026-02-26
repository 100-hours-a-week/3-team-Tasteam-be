package com.tasteam.domain.restaurant.dto.request;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리뷰 목록 아이템")
public record ReviewResponse(
	@Schema(description = "리뷰 ID", example = "101")
	long id,
	@Schema(description = "그룹 ID", example = "1")
	long groupId,
	@Schema(description = "하위그룹 ID (없으면 null)", example = "2")
	Long subgroupId,
	@Schema(description = "그룹명", example = "맛집 탐방반")
	String groupName,
	@Schema(description = "하위그룹명 (없으면 null)", example = "강남구 모임")
	String subgroupName,
	@Schema(description = "작성자 정보")
	AuthorResponse author,
	@Schema(description = "리뷰 본문 요약", example = "가격 대비 맛있어요.")
	String contentPreview,
	@Schema(description = "추천 여부", example = "true")
	boolean isRecommended,
	@Schema(description = "키워드 목록")
	List<String> keywords,
	@Schema(description = "썸네일 이미지 목록(최대 3장)")
	List<ReviewImageResponse> thumbnailImages,
	@Schema(description = "작성 시각", example = "2025-12-31T15:00:00Z")
	Instant createdAt,
	@Schema(description = "음식점 ID (하위그룹 리뷰 목록 등에서 제공)", example = "1")
	Long restaurantId,
	@Schema(description = "음식점명 (하위그룹 리뷰 목록 등에서 제공)", example = "맛있는 밥집")
	String restaurantName,
	@Schema(description = "그룹 로고 이미지 URL (맥락 표시용)")
	String groupLogoImageUrl,
	@Schema(description = "그룹 주소 (음식점 상세 리뷰 맥락 표시용, address만)")
	String groupAddress,
	@Schema(description = "음식점 대표 이미지 URL (하위그룹 리뷰 맥락 표시용)")
	String restaurantImageUrl,
	@Schema(description = "음식점 주소 (하위그룹 리뷰 맥락 표시용)")
	String restaurantAddress) {

	@Schema(description = "작성자 요약")
	public record AuthorResponse(
		@Schema(description = "작성자 닉네임", example = "맛객123")
		String nickname,
		@Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/profile/10.jpg")
		String profileImageUrl) {
	}

	@Schema(description = "리뷰 이미지")
	public record ReviewImageResponse(
		@Schema(description = "이미지 ID", example = "501")
		long id,
		@Schema(description = "이미지 URL", example = "https://cdn.example.com/review/501.jpg")
		String url) {
	}
}
