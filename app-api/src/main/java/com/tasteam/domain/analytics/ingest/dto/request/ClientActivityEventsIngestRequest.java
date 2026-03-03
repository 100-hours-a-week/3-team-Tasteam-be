package com.tasteam.domain.analytics.ingest.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ClientActivityEventsIngestRequest(
	String anonymousId,
	@NotEmpty(message = "events는 최소 1건 이상이어야 합니다.")
	List<@Valid ClientActivityEventItemRequest> events) {
}
