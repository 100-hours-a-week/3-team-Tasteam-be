package com.tasteam.domain.member.dto.response;

public record GroupRequestPreviewResponse(
		Long id,
		String groupName,
		String groupAddress,
		String status) {
}
