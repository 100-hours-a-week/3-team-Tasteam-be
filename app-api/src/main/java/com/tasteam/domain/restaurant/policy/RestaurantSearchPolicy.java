package com.tasteam.domain.restaurant.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestaurantSearchPolicy {
	public static final int DEFAULT_RADIUS_METER = 3_000;
	public static final int MAX_RADIUS_METER = 3_000;

	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 20;
}
