package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record GroupMemberListItem(
	@JsonIgnore
	Long cursorId,
	Long memberId,
	String nickname,
	ProfileImage profileImage,
	Instant createdAt) {

	public record ProfileImage(
		java.util.UUID id,
		String url) {
	}
}
