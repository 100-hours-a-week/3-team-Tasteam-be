package com.tasteam.global.dto.pagination;

public record OffsetPagination(
	int page,
	int size,
	int totalPages,
	int totalElements) {
}
