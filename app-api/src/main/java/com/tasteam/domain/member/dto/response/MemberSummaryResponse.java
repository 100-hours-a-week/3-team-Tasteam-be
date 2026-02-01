package com.tasteam.domain.member.dto.response;

import com.tasteam.domain.member.entity.Member;

public record MemberSummaryResponse(
	String nickname,
	String profileImageUrl) {

	public static MemberSummaryResponse from(Member member, String profileImageUrl) {
		return new MemberSummaryResponse(
			member.getNickname(),
			profileImageUrl);
	}
}
