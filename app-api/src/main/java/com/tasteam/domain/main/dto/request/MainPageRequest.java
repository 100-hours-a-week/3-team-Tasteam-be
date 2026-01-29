package com.tasteam.domain.main.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record MainPageRequest(
	@DecimalMin(value = "-90.0", inclusive = true) @DecimalMax(value = "90.0", inclusive = true)
	Double latitude,
	@DecimalMin(value = "-180.0", inclusive = true) @DecimalMax(value = "180.0", inclusive = true)
	Double longitude) {

	public boolean hasLocation() {
		return latitude != null && longitude != null;
	}
}
