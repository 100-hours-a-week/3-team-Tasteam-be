package com.tasteam.domain.analytics.ingest.dto.request;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record ClientActivityEventItemRequest(
	@NotBlank(message = "eventId는 필수입니다.")
	String eventId,
	@NotBlank(message = "eventName은 필수입니다.")
	String eventName,
	String eventVersion,
	Instant occurredAt,
	Map<String, Object> properties) {
}
