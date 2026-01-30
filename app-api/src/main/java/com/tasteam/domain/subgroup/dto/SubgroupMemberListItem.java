package com.tasteam.domain.subgroup.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record SubgroupMemberListItem(
	@JsonIgnore
	Long cursorId,
	Long memberId,
	String nickname,
	String profileImageUrl,
	Instant createdAt) {
}
