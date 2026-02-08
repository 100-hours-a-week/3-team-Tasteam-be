package com.tasteam.domain.member.dto.response;

import com.tasteam.domain.member.entity.Member;

public record MemberMeResponse(
	MemberSummaryResponse member,
	MemberPreviewResponse<GroupRequestPreviewResponse> groupRequests,
	MemberPreviewResponse<ReviewSummaryResponse> reviews) {
	public static MemberMeResponse from(Member member, String profileImageUrl) {
		return new MemberMeResponse(
			MemberSummaryResponse.from(member, profileImageUrl),
			MemberPreviewResponse.empty(),
			MemberPreviewResponse.empty());
	}
}
