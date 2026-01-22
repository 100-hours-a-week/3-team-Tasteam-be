package com.tasteam.domain.search.dto.response;

import java.util.List;

public record SearchSectionResponse(
	String type,
	List<Long> restaurantIds) {
}
