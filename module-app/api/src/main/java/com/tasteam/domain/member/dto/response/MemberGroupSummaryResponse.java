package com.tasteam.domain.member.dto.response;

import java.util.List;

public record MemberGroupSummaryResponse(
	Long groupId,
	String groupName,
	List<MemberSubgroupSummaryResponse> subGroups) {
}
