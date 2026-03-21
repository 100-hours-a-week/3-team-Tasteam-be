package com.tasteam.domain.announcement.dto.response;

import java.time.Instant;

import com.tasteam.domain.announcement.entity.Announcement;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnnouncementDetailResponse(
	@Schema(description = "공지 ID", example = "1")
	Long id,

	@Schema(description = "공지 제목", example = "서비스 점검 안내")
	String title,

	@Schema(description = "공지 본문")
	String content,

	@Schema(description = "작성일시")
	Instant createdAt,

	@Schema(description = "수정일시")
	Instant updatedAt) {
	public static AnnouncementDetailResponse of(Announcement notice) {
		return new AnnouncementDetailResponse(
			notice.getId(),
			notice.getTitle(),
			notice.getContent(),
			notice.getCreatedAt(),
			notice.getUpdatedAt());
	}
}
