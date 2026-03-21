package com.tasteam.domain.restaurant.dto;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum FoodCategoryCode {
	KOREAN,
	CHINESE,
	JAPANESE,
	WESTERN,
	ASIAN,
	SNACK,
	FAST_FOOD,
	CAFE,
	DESSERT,
	BAR,
	BBQ;

	public static Set<String> codes() {
		return Arrays.stream(values())
			.map(Enum::name)
			.collect(Collectors.toUnmodifiableSet());
	}
}
