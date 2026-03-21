package com.tasteam.domain.member.dto.response;

import java.util.List;

public record MemberGroupDetailSummaryResponse(
	Long groupId,
	String groupName,
	String groupAddress,
	String groupDetailAddress,
	String groupLogoImageUrl,
	long groupMemberCount,
	List<MemberSubgroupDetailSummaryResponse> subGroups) {
}
