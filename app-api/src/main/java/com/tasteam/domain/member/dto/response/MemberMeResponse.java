package com.tasteam.domain.member.dto.response;

import com.tasteam.domain.member.entity.Member;

public record MemberMeResponse(
	MemberSummaryResponse member,
	MemberPreviewResponse<GroupRequestPreviewResponse> groupRequests,
	MemberPreviewResponse<ReviewSummaryResponse> reviews) {
	public static MemberMeResponse from(Member member) {
		return new MemberMeResponse(
			MemberSummaryResponse.from(member),
			MemberPreviewResponse.empty(),
			MemberPreviewResponse.empty());
	}
}
