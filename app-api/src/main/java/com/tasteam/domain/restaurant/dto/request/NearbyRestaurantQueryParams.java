package com.tasteam.domain.restaurant.dto.request;

import java.util.List;

import jakarta.validation.constraints.*;

public record NearbyRestaurantQueryParams(

	@NotNull @DecimalMin(value = "-90.0", inclusive = true) @DecimalMax(value = "90.0", inclusive = true)
	Double lat,

	@NotNull @DecimalMin(value = "-180.0", inclusive = true) @DecimalMax(value = "180.0", inclusive = true)
	Double lng,

	@Min(1)
	Integer radius,

	List<@NotBlank String> categories,

	String cursor,

	@Min(1)
	Integer size) {
}
