package com.tasteam.domain.restaurant.service.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RatioConverter {

	private RatioConverter() {}

	public static BigDecimal percentageToRatio(int percentage) {
		int bounded = Math.max(0, Math.min(percentage, 100));
		return BigDecimal.valueOf(bounded)
			.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
	}
}
