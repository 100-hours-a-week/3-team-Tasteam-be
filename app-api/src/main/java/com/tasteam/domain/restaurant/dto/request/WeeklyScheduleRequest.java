package com.tasteam.domain.restaurant.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

public record WeeklyScheduleRequest(
	Integer dayOfWeek,
	LocalTime openTime,
	LocalTime closeTime,
	Boolean isClosed,
	LocalDate effectiveFrom,
	LocalDate effectiveTo) {
}
