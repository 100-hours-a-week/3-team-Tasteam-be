package com.tasteam.domain.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.policy.AdminAuthPolicy;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.domain.restaurant.service.RestaurantScheduleService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/restaurants/{restaurantId}/schedules")
public class AdminScheduleController {

	private final RestaurantScheduleService restaurantScheduleService;
	private final AdminAuthPolicy adminAuthPolicy;
	private final MemberRepository memberRepository;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<List<BusinessHourWeekItem>> getSchedules(
		@PathVariable
		Long restaurantId,
		@CurrentUser
		Long memberId) {

		Member member = memberRepository.findById(memberId).orElseThrow();
		adminAuthPolicy.validateAdmin(member);

		List<BusinessHourWeekItem> result = restaurantScheduleService.getBusinessHoursWeek(restaurantId);
		return SuccessResponse.success(result);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Void> createSchedules(
		@PathVariable
		Long restaurantId,
		@Validated @RequestBody
		List<WeeklyScheduleRequest> request,
		@CurrentUser
		Long memberId) {

		Member member = memberRepository.findById(memberId).orElseThrow();
		adminAuthPolicy.validateAdmin(member);

		restaurantScheduleService.createWeeklySchedules(restaurantId, request);
		return SuccessResponse.success(null);
	}
}
