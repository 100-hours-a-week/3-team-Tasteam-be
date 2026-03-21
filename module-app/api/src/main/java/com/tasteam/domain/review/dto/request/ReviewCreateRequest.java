package com.tasteam.domain.review.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
	@NotNull @Positive
	Long groupId,
	Long subgroupId,
	@Size(max = 1000)
	String content,
	@NotNull
	Boolean isRecommended,
	@NotNull @Size(min = 1)
	List<Long> keywordIds,
	List<UUID> imageIds) {
}
