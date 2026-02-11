package com.tasteam.domain.admin.dto.response;

import java.time.Instant;

public record AdminAnnouncementListItem(
	Long id,
	String title,
	Instant createdAt,
	Instant updatedAt) {
}
