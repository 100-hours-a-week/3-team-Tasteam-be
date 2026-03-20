package com.tasteam.domain.admin.controller.docs;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(110)
@Tag(name = "Admin - Restaurant", description = "어드민 음식점 관리 API")
public interface AdminScheduleControllerDocs {

	@Operation(summary = "영업 스케줄 조회", description = "음식점의 주간 영업 스케줄을 조회합니다.")
	SuccessResponse<List<BusinessHourWeekItem>> getSchedules(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId);

	@Operation(summary = "영업 스케줄 등록", description = "음식점의 주간 영업 스케줄을 등록합니다.")
	SuccessResponse<Void> createSchedules(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId,
		@Validated @RequestBody
		List<WeeklyScheduleRequest> request);
}
