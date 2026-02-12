package com.tasteam.domain.admin.dto.response;

import java.time.Instant;

public record AdminAnnouncementDetailResponse(
	Long id,
	String title,
	String content,
	Instant createdAt,
	Instant updatedAt) {
}
