package com.tasteam.domain.group.type;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GroupType {
	OFFICIAL,
	UNOFFICIAL;

	@JsonCreator
	public static GroupType from(String raw) {
		if (raw == null) {
			return null;
		}

		String value = raw.trim().toUpperCase();
		return switch (value) {
			case "OFFICIAL" -> OFFICIAL;
			case "UNOFFICIAL" -> UNOFFICIAL;
			// Alias: legacy clients may send SCHOOL to indicate an official (school‑verified) group
			case "SCHOOL" -> OFFICIAL;
			default -> throw new IllegalArgumentException(
				"Invalid GroupType: " + raw + " (accepted: OFFICIAL, UNOFFICIAL, alias: SCHOOL -> OFFICIAL)");
		};
	}
}
