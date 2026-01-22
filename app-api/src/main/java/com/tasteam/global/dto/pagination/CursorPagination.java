package com.tasteam.global.dto.pagination;

public record CursorPagination(
	String nextCursor,
	int size,
	boolean hasNext) {
	public static CursorPagination empty() {
		return new CursorPagination(null, 0, false);
	}
}
