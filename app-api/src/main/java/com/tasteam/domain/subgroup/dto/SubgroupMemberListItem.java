package com.tasteam.domain.subgroup.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record SubgroupMemberListItem(
	@JsonIgnore
	Long cursorId,
	Long memberId,
	String nickname,
	ProfileImage profileImage,
	Instant createdAt) {

	public SubgroupMemberListItem(
		Long cursorId,
		Long memberId,
		String nickname,
		UUID profileImageUuid,
		String profileImageUrl,
		Instant createdAt) {
		this(
			cursorId,
			memberId,
			nickname,
			new ProfileImage(profileImageUuid, profileImageUrl),
			createdAt);
	}

	public record ProfileImage(
		UUID id,
		String url) {
	}
}
