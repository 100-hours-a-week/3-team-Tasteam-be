package com.tasteam.domain.main.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record MainPageRequest(
	@NotNull @DecimalMin(value = "-90.0", inclusive = true) @DecimalMax(value = "90.0", inclusive = true)
	Double latitude,
	@NotNull @DecimalMin(value = "-180.0", inclusive = true) @DecimalMax(value = "180.0", inclusive = true)
	Double longitude) {
}
