package com.tasteam.domain.member.dto.response;

import com.tasteam.domain.member.entity.Member;

public record MemberMeResponse(
	MemberSummaryResponse member,
	MemberPreviewResponse<GroupRequestPreviewResponse> groupRequests,
	MemberPreviewResponse<ReviewSummaryResponse> reviews) {
	public static MemberMeResponse of(Member member, MemberSummaryResponse.ProfileImage profileImage) {
		return new MemberMeResponse(
			MemberSummaryResponse.of(member, profileImage),
			MemberPreviewResponse.empty(),
			MemberPreviewResponse.empty());
	}
}
