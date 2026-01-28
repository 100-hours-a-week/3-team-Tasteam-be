package com.tasteam.domain.restaurant.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.tasteam.domain.restaurant.type.DayOfWeekCode;
import com.tasteam.domain.restaurant.type.ScheduleSource;

public record BusinessHourWeekItem(
	String date,
	String dayOfWeek,
	Boolean isClosed,
	String openTime,
	String closeTime,
	String source,
	String reason) {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	public static BusinessHourWeekItem from(
		LocalDate date,
		DayOfWeekCode dayOfWeek,
		Boolean isClosed,
		LocalTime openTime,
		LocalTime closeTime,
		ScheduleSource source,
		String reason) {

		return new BusinessHourWeekItem(
			date.format(DATE_FORMATTER),
			dayOfWeek.name(),
			isClosed,
			openTime != null ? openTime.format(TIME_FORMATTER) : null,
			closeTime != null ? closeTime.format(TIME_FORMATTER) : null,
			source.name(),
			reason);
	}
}
