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

	public record ProfileImage(
		UUID id,
		String url) {
	}
}
