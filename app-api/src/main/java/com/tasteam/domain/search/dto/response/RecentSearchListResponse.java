package com.tasteam.domain.search.dto.response;

import java.time.Instant;
import java.util.List;

public record RecentSearchListResponse(
	List<RecentSearchItem> data,
	PageInfo page) {

	public record RecentSearchItem(
		long id,
		String keyword,
		long count,
		Instant updatedAt) {
	}

	public record PageInfo(
		String nextCursor,
		int size,
		boolean hasNext) {
	}
}
