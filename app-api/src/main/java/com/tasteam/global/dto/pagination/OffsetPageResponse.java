package com.tasteam.global.dto.pagination;

import java.util.List;

public record OffsetPageResponse<T>(
	List<T> items,
	OffsetPagination pagination) {
}
