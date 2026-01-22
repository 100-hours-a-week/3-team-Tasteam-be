package com.tasteam.domain.search.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
	@NotBlank @Size(min = 1, max = 64)
	String keyword,
	String cursor,
	@Min(1) @Max(100)
	Integer size) {
}
