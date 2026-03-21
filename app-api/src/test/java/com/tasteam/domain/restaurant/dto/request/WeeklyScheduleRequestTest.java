package com.tasteam.domain.restaurant.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@DisplayName("[유닛](Weekly) WeeklyScheduleRequest 단위 테스트")
class WeeklyScheduleRequestTest {

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	@DisplayName("숫자 문자열 요일은 정수 요일 코드로 역직렬화된다")
	void deserializesNumericString() throws Exception {
		String json = """
			{"dayOfWeek":"3"}
			""";

		WeeklyScheduleRequest request = objectMapper.readValue(json, WeeklyScheduleRequest.class);

		assertThat(request.dayOfWeek()).isEqualTo(3);
	}

	@Test
	@DisplayName("영문 전체 요일명은 정수 요일 코드로 역직렬화된다")
	void deserializesFullDayName() throws Exception {
		String json = """
			{"dayOfWeek":"MONDAY"}
			""";

		WeeklyScheduleRequest request = objectMapper.readValue(json, WeeklyScheduleRequest.class);

		assertThat(request.dayOfWeek()).isEqualTo(1);
	}

	@Test
	@DisplayName("영문 축약 요일명은 정수 요일 코드로 역직렬화된다")
	void deserializesShortDayCode() throws Exception {
		String json = """
			{"dayOfWeek":"SUN"}
			""";

		WeeklyScheduleRequest request = objectMapper.readValue(json, WeeklyScheduleRequest.class);

		assertThat(request.dayOfWeek()).isEqualTo(7);
	}

	@Test
	@DisplayName("범위를 벗어난 숫자 요일은 예외가 발생한다")
	void rejectsOutOfRangeNumbers() {
		String json = """
			{"dayOfWeek":9}
			""";

		assertThatThrownBy(() -> objectMapper.readValue(json, WeeklyScheduleRequest.class))
			.isInstanceOf(InvalidFormatException.class)
			.hasMessageContaining("between 1 (Monday) and 7 (Sunday)");
	}

	@Test
	@DisplayName("알 수 없는 요일 문자열은 예외가 발생한다")
	void rejectsUnknownStrings() {
		String json = """
			{"dayOfWeek":"FUNDAY"}
			""";

		assertThatThrownBy(() -> objectMapper.readValue(json, WeeklyScheduleRequest.class))
			.isInstanceOf(InvalidFormatException.class)
			.hasMessageContaining("dayOfWeek must be an integer 1-7 or a day name");
	}
}
