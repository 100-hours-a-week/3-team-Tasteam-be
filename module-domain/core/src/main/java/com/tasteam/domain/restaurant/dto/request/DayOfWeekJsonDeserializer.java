package com.tasteam.domain.restaurant.dto.request;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tasteam.domain.restaurant.type.DayOfWeekCode;

/**
 * Allows {@code dayOfWeek} to be provided as 1-7, a numeric string, or a day name (e.g. MONDAY/MON).
 */
public class DayOfWeekJsonDeserializer extends JsonDeserializer<Integer> {

	@Override
	public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		JsonToken token = parser.currentToken();

		if (token == JsonToken.VALUE_NULL) {
			return null;
		}

		if (token == JsonToken.VALUE_NUMBER_INT) {
			return validateRange(parser, parser.getIntValue());
		}

		if (token == JsonToken.VALUE_STRING) {
			String raw = parser.getText();
			if (raw == null) {
				return null;
			}

			String normalized = raw.trim();
			if (normalized.isEmpty()) {
				return null;
			}

			String upper = normalized.toUpperCase(Locale.US);

			// Numeric string (e.g. "3")
			try {
				return validateRange(parser, Integer.parseInt(upper));
			} catch (NumberFormatException ignored) {
				// fall through
			}

			// Full day name (e.g. MONDAY)
			try {
				return DayOfWeek.valueOf(upper).getValue();
			} catch (IllegalArgumentException ignored) {
				// fall through
			}

			// 3-letter code (e.g. MON)
			String shortCode = upper.length() >= 3 ? upper.substring(0, 3) : upper;
			try {
				return DayOfWeekCode.valueOf(shortCode).getValue();
			} catch (IllegalArgumentException ignored) {
				// fall through
			}
		}

		throw InvalidFormatException.from(parser,
			"dayOfWeek must be an integer 1-7 or a day name (e.g. MONDAY, MON)",
			parser.getText(),
			Integer.class);
	}

	private Integer validateRange(JsonParser parser, int value) throws IOException {
		if (value < 1 || value > 7) {
			throw InvalidFormatException.from(parser,
				"dayOfWeek must be between 1 (Monday) and 7 (Sunday)",
				value,
				Integer.class);
		}
		return value;
	}
}
