package com.tasteam.domain.restaurant.service;

import java.util.List;

public record CursorQueryResult<T>(
	List<T> items,
	String nextCursor,
	boolean hasNext,
	int size) {
}
