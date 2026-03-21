package com.tasteam.domain.announcement.dto.response;

import java.time.Instant;

import com.tasteam.domain.announcement.entity.Announcement;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnnouncementListResponse(
	@Schema(description = "공지 ID", example = "1")
	Long id,

	@Schema(description = "공지 제목", example = "서비스 점검 안내")
	String title,

	@Schema(description = "작성일시")
	Instant createdAt) {
	public static AnnouncementListResponse of(Announcement notice) {
		return new AnnouncementListResponse(
			notice.getId(),
			notice.getTitle(),
			notice.getCreatedAt());
	}
}
