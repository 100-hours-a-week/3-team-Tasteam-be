package com.tasteam.domain.member.dto.response;

public record MemberGroupDetailSummaryRow(
	Long groupId,
	String groupName,
	String groupAddress,
	String groupDetailAddress,
	String groupLogoImageUrl,
	long groupMemberCount) {
}
