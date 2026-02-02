package com.tasteam.domain.admin.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRestaurantUpdateRequest(
	@NotBlank(message = "음식점 이름은 필수입니다") @Size(max = 100, message = "음식점 이름은 최대 100자입니다")
	String name,

	@NotBlank(message = "주소는 필수입니다") @Size(max = 255, message = "주소는 최대 255자입니다")
	String address,

	List<Long> foodCategoryIds,

	List<UUID> imageIds) {
}
