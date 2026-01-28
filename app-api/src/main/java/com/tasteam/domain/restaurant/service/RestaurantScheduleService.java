package com.tasteam.domain.restaurant.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.domain.restaurant.entity.RestaurantScheduleOverride;
import com.tasteam.domain.restaurant.entity.RestaurantWeeklySchedule;
import com.tasteam.domain.restaurant.repository.RestaurantScheduleOverrideRepository;
import com.tasteam.domain.restaurant.repository.RestaurantWeeklyScheduleRepository;
import com.tasteam.domain.restaurant.type.DayOfWeekCode;
import com.tasteam.domain.restaurant.type.ScheduleSource;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RestaurantScheduleService {

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final int BUSINESS_HOURS_WEEK_SIZE = 7;

	private final RestaurantWeeklyScheduleRepository weeklyScheduleRepository;
	private final RestaurantScheduleOverrideRepository scheduleOverrideRepository;

	@Transactional(readOnly = true)
	public List<BusinessHourWeekItem> getBusinessHoursWeek(Long restaurantId) {
		LocalDate today = LocalDate.now(KOREA_ZONE);
		LocalDate endDate = today.plusDays(BUSINESS_HOURS_WEEK_SIZE - 1);

		Map<LocalDate, RestaurantScheduleOverride> overridesMap = scheduleOverrideRepository
			.findByRestaurantIdAndDateRange(restaurantId, today, endDate)
			.stream()
			.collect(Collectors.toMap(
				RestaurantScheduleOverride::getDate,
				Function.identity()));

		Set<Integer> dayOfWeeks = IntStream.range(0, BUSINESS_HOURS_WEEK_SIZE)
			.mapToObj(today::plusDays)
			.map(date -> date.getDayOfWeek().getValue())
			.collect(Collectors.toSet());

		Map<Integer, RestaurantWeeklySchedule> weeklyMap = weeklyScheduleRepository
			.findEffectiveSchedules(restaurantId, dayOfWeeks, today)
			.stream()
			.collect(Collectors.toMap(
				RestaurantWeeklySchedule::getDayOfWeek,
				Function.identity(),
				(existing, replacement) -> existing));

		List<BusinessHourWeekItem> result = new ArrayList<>();

		for (int i = 0; i < BUSINESS_HOURS_WEEK_SIZE; i++) {
			LocalDate targetDate = today.plusDays(i);
			int dayOfWeekValue = targetDate.getDayOfWeek().getValue();
			DayOfWeekCode dayOfWeekCode = DayOfWeekCode.fromValue(dayOfWeekValue);

			RestaurantScheduleOverride override = overridesMap.get(targetDate);
			if (override != null) {
				result.add(BusinessHourWeekItem.from(
					targetDate,
					dayOfWeekCode,
					override.getIsClosed(),
					override.getOpenTime(),
					override.getCloseTime(),
					ScheduleSource.OVERRIDE,
					override.getReason()));
				continue;
			}

			RestaurantWeeklySchedule weekly = weeklyMap.get(dayOfWeekValue);
			if (weekly != null) {
				result.add(BusinessHourWeekItem.from(
					targetDate,
					dayOfWeekCode,
					weekly.getIsClosed(),
					weekly.getOpenTime(),
					weekly.getCloseTime(),
					ScheduleSource.WEEKLY,
					null));
				continue;
			}

			result.add(BusinessHourWeekItem.from(
				targetDate,
				dayOfWeekCode,
				null,
				null,
				null,
				ScheduleSource.NONE,
				null));
		}

		return result;
	}
}
