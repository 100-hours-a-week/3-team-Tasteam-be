package com.tasteam.domain.admin.dto.request;

import java.util.List;
import java.util.UUID;

import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;

import jakarta.validation.constraints.NotBlank;

public record AdminRestaurantCreateRequest(
	// 음식점 테이블 필수 컬럼만 검증하고 나머지는 선택으로 둔다.
	@NotBlank(message = "음식점 이름은 필수입니다")
	String name,
	@NotBlank(message = "주소는 필수입니다")
	String address,
	List<Long> foodCategoryIds,

	List<UUID> imageIds,

	List<WeeklyScheduleRequest> weeklySchedules) {
}
