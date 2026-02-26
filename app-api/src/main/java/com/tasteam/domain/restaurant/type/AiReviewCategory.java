package com.tasteam.domain.restaurant.type;

import java.util.Map;
import java.util.Optional;

/**
 * 리뷰 요약/비교 분석에서 사용하는 카테고리.
 * JSON/DB 키(food, price, service)와 enum 매핑.
 */
public enum AiReviewCategory {
	TASTE("food", "맛"),
	PRICE("price", "가격"),
	SERVICE("service", "서비스");

	private final String jsonKey;
	private final String displayName;

	AiReviewCategory(String jsonKey, String displayName) {
		this.jsonKey = jsonKey;
		this.displayName = displayName;
	}

	public String getJsonKey() {
		return jsonKey;
	}

	public String getDisplayName() {
		return displayName;
	}

	private static final Map<String, AiReviewCategory> BY_JSON_KEY = Map.of(
		"food", TASTE,
		"price", PRICE,
		"service", SERVICE);

	public static Optional<AiReviewCategory> fromJsonKey(String key) {
		if (key == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(BY_JSON_KEY.get(key.toLowerCase()));
	}
}
