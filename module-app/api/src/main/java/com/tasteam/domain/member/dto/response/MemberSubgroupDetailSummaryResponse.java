package com.tasteam.domain.member.dto.response;

public record MemberSubgroupDetailSummaryResponse(
	Long subGroupId,
	String subGroupName,
	Integer memberCount,
	String logoImageUrl) {
}
