package com.tasteam.domain.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
	@NotNull
	Long categoryId,

	@NotBlank @Size(max = 100)
	String name,

	@Size(max = 500)
	String description,

	@NotNull @PositiveOrZero
	Integer price,

	@Size(max = 500)
	String imageUrl,

	Boolean isRecommended,

	@NotNull @PositiveOrZero
	Integer displayOrder) {
}
