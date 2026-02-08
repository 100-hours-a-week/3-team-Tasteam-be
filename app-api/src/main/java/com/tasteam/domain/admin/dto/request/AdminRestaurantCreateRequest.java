package com.tasteam.domain.admin.dto.request;

import java.util.List;
import java.util.UUID;

import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;

import jakarta.validation.constraints.NotBlank;

public record AdminRestaurantCreateRequest(
	@NotBlank(message = "음식점 이름은 필수입니다")
	String name,
	@NotBlank(message = "주소는 필수입니다")
	String address,
	String phoneNumber,
	List<Long> foodCategoryIds,
	List<UUID> imageIds,
	List<WeeklyScheduleRequest> weeklySchedules) {
}
