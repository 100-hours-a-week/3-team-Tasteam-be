package com.tasteam.domain.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuCategoryCreateRequest(
	@NotBlank @Size(max = 50)
	String name,

	@NotNull @PositiveOrZero
	Integer displayOrder) {
}
