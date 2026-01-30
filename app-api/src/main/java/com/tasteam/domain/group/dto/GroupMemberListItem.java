package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record GroupMemberListItem(
	@JsonIgnore
	Long cursorId,
	Long memberId,
	String nickname,
	String profileImageUrl,
	Instant createdAt) {
}
