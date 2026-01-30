package com.tasteam.domain.member.dto.response;

import java.util.UUID;

import com.tasteam.domain.member.entity.Member;

public record MemberSummaryResponse(
	String nickname,
	ProfileImage profileImage) {

	public static MemberSummaryResponse of(Member member, ProfileImage profileImage) {
		return new MemberSummaryResponse(
			member.getNickname(),
			profileImage);
	}

	public record ProfileImage(
		UUID id,
		String url) {
	}
}
