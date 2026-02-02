package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "커서 기반 페이지 응답")
public record CursorPageResponse<T>(
	@Schema(description = "응답 아이템 목록")
	List<T> items,
	@Schema(description = "페이지네이션 정보")
	Pagination pagination) {

	@Schema(description = "페이지네이션")
	public record Pagination(
		@Schema(description = "다음 페이지 조회용 커서", example = "eyJpZCI6MTAwfQ==")
		String nextCursor,
		@Schema(description = "다음 페이지 존재 여부", example = "true")
		Boolean hasNext,
		@Schema(description = "현재 페이지 아이템 수", example = "10")
		Integer size) {
	}

	public static <T> CursorPageResponse<T> empty() {
		return new CursorPageResponse<>(
			List.of(),
			new Pagination(null, false, 0));
	}
}
