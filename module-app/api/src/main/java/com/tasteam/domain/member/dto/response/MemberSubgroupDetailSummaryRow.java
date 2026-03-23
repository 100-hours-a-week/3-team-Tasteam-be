package com.tasteam.domain.member.dto.response;

public record MemberSubgroupDetailSummaryRow(
	Long groupId,
	Long subGroupId,
	String subGroupName,
	Integer memberCount,
	String logoImageUrl) {
}
