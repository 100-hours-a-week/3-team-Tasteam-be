package com.tasteam.domain.restaurant.dto.request;

import com.tasteam.global.validation.ValidationPatterns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
	@Pattern(regexp = ValidationPatterns.UUID_PATTERN, message = "imageFileUuid 형식이 올바르지 않습니다")
	String imageFileUuid,

	Boolean isRecommended,

	@NotNull @PositiveOrZero
	Integer displayOrder) {
}
