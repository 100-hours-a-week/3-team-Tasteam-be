package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

public record CursorPageResponse<T>(
	List<T> items,
	Pagination pagination) {
	public record Pagination(
		String nextCursor,
		Boolean hasNext,
		Integer size) {
	}

	public static <T> CursorPageResponse<T> empty() {
		return new CursorPageResponse<>(
			List.of(),
			new Pagination(null, false, 0));
	}
}
