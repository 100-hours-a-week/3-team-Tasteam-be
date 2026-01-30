package com.tasteam.domain.search.dto.response;

public record SearchGroupSummary(
	long groupId,
	String name,
	LogoImage logoImage,
	long memberCount) {

	public record LogoImage(
		java.util.UUID id,
		String url) {
	}
}
