package com.tasteam.domain.admin.dto.request;

import java.util.List;
import java.util.UUID;

import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AdminRestaurantCreateRequest(
	@NotBlank(message = "음식점 이름은 필수입니다") @Size(max = 100, message = "음식점 이름은 최대 100자입니다")
	String name,

	@NotBlank(message = "주소는 필수입니다") @Size(max = 255, message = "주소는 최대 255자입니다")
	String address,

	@NotEmpty(message = "음식 카테고리는 최소 1개 이상 선택해야 합니다")
	List<Long> foodCategoryIds,

	List<UUID> imageIds,

	List<WeeklyScheduleRequest> weeklySchedules) {
}
