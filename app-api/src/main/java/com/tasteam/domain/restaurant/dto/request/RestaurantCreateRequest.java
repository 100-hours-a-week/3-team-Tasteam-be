package com.tasteam.domain.restaurant.dto.request;

import java.util.List;
import java.util.UUID;

public record RestaurantCreateRequest(
	String name,
	String address,
	String phoneNumber,
	List<Long> foodCategoryIds,
	List<UUID> imageIds,
	List<WeeklyScheduleRequest> weeklySchedules) {
}
