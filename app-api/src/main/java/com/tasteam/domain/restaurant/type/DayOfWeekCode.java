package com.tasteam.domain.restaurant.type;

import java.time.LocalDate;

public enum DayOfWeekCode {
	MON(1),
	TUE(2),
	WED(3),
	THU(4),
	FRI(5),
	SAT(6),
	SUN(7);

	private final int value;

	DayOfWeekCode(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static DayOfWeekCode fromValue(int value) {
		for (DayOfWeekCode day : values()) {
			if (day.value == value) {
				return day;
			}
		}
		throw new IllegalArgumentException("Invalid day of week value: " + value);
	}

	public static DayOfWeekCode fromLocalDate(LocalDate date) {
		int isoDayOfWeek = date.getDayOfWeek().getValue();
		return fromValue(isoDayOfWeek);
	}
}
