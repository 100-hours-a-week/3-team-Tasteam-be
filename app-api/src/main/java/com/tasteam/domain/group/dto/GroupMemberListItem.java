package com.tasteam.domain.group.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@Getter
public class GroupMemberListItem {

	@JsonIgnore
	private final Long cursorId;
	private final Long memberId;
	private final String nickname;
	private final String profileImageUrl;
	private final Instant createdAt;

	public GroupMemberListItem(
		Long cursorId,
		Long memberId,
		String nickname,
		String profileImageUrl,
		Instant createdAt
	) {
		this.cursorId = cursorId;
		this.memberId = memberId;
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
		this.createdAt = createdAt;
	}
}
