package com.tasteam.domain.restaurant.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class WeeklyScheduleRequestTest {

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	void deserializesNumericString() throws Exception {
		String json = """
			{"dayOfWeek":"3"}
			""";

		WeeklyScheduleRequest request = objectMapper.readValue(json, WeeklyScheduleRequest.class);

		assertThat(request.dayOfWeek()).isEqualTo(3);
	}

	@Test
	void deserializesFullDayName() throws Exception {
		String json = """
			{"dayOfWeek":"MONDAY"}
			""";

		WeeklyScheduleRequest request = objectMapper.readValue(json, WeeklyScheduleRequest.class);

		assertThat(request.dayOfWeek()).isEqualTo(1);
	}

	@Test
	void deserializesShortDayCode() throws Exception {
		String json = """
			{"dayOfWeek":"SUN"}
			""";

		WeeklyScheduleRequest request = objectMapper.readValue(json, WeeklyScheduleRequest.class);

		assertThat(request.dayOfWeek()).isEqualTo(7);
	}

	@Test
	void rejectsOutOfRangeNumbers() {
		String json = """
			{"dayOfWeek":9}
			""";

		assertThatThrownBy(() -> objectMapper.readValue(json, WeeklyScheduleRequest.class))
			.isInstanceOf(InvalidFormatException.class)
			.hasMessageContaining("between 1 (Monday) and 7 (Sunday)");
	}

	@Test
	void rejectsUnknownStrings() {
		String json = """
			{"dayOfWeek":"FUNDAY"}
			""";

		assertThatThrownBy(() -> objectMapper.readValue(json, WeeklyScheduleRequest.class))
			.isInstanceOf(InvalidFormatException.class)
			.hasMessageContaining("dayOfWeek must be an integer 1-7 or a day name");
	}
}
