package com.tasteam.domain.member.dto.response;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tasteam.global.dto.pagination.OffsetPagination;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberPreviewResponse<T>(
	List<T> data,
	OffsetPagination pagination) {
	public static <T> MemberPreviewResponse<T> empty() {
		return new MemberPreviewResponse<>(
			Collections.emptyList(),
			new OffsetPagination(0, 0, 0, 0));
	}
}
