package com.tasteam.domain.member.dto.response;

public record MemberSubgroupSummaryRow(
	Long groupId,
	Long subGroupId,
	String subGroupName) {
}
