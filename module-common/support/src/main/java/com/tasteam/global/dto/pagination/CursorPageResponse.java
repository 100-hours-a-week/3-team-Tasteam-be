package com.tasteam.global.dto.pagination;

import java.util.List;

public record CursorPageResponse<T>(
	List<T> items,
	CursorPagination pagination) {
}
