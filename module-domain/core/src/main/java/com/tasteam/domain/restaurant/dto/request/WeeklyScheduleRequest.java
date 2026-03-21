package com.tasteam.domain.restaurant.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public record WeeklyScheduleRequest(
	@JsonDeserialize(using = DayOfWeekJsonDeserializer.class)
	Integer dayOfWeek,
	LocalTime openTime,
	LocalTime closeTime,
	Boolean isClosed,
	LocalDate effectiveFrom,
	LocalDate effectiveTo) {
}
