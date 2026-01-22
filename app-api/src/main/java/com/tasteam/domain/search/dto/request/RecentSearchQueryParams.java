package com.tasteam.domain.search.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RecentSearchQueryParams(
	String cursor,
	@Min(1) @Max(100)
	Integer size) {
}
