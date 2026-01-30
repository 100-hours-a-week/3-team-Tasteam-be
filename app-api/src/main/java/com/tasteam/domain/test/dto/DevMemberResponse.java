package com.tasteam.domain.test.dto;

import java.util.List;

import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;

public record DevMemberResponse(
	Long memberId,
	String email,
	String nickname,
	String profileImageUrl,
	List<MemberGroupSummaryResponse> groups) {
}
