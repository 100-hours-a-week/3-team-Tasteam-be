package com.tasteam.domain.member.dto.response;

import java.util.UUID;

import com.tasteam.domain.member.entity.Member;

public record MemberSummaryResponse(
	String nickname,
	ProfileImage profileImage) {

	public static MemberSummaryResponse from(Member member) {
		return new MemberSummaryResponse(
			member.getNickname(),
			toProfileImage(member));
	}

	private static ProfileImage toProfileImage(Member member) {
		if (member.getProfileImageUuid() == null || member.getProfileImageUrl() == null) {
			return null;
		}
		return new ProfileImage(member.getProfileImageUuid(), member.getProfileImageUrl());
	}

	public record ProfileImage(
		UUID id,
		String url) {
	}
}
