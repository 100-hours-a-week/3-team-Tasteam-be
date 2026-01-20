package com.tasteam.domain.member.dto.response;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tasteam.global.dto.api.PaginationResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberPreviewResponse<T>(
		List<T> data,
		PaginationResponse page) {
	public static <T> MemberPreviewResponse<T> empty() {
		return new MemberPreviewResponse<>(
			Collections.emptyList(),
			PaginationResponse.builder()
				.size(0)
				.hasNext(false)
				.build());
	}
}
