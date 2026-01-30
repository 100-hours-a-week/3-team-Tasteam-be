package com.tasteam.domain.search.dto.response;

public record SearchGroupSummary(
	long groupId,
	String name,
	String logoImageUrl,
	long memberCount) {
}
