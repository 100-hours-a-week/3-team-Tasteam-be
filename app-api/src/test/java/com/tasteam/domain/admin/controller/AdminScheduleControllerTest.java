package com.tasteam.domain.admin.controller;

import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseAdminControllerWebMvcTest;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.domain.restaurant.type.DayOfWeekCode;
import com.tasteam.domain.restaurant.type.ScheduleSource;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

@DisplayName("[유닛](Admin) AdminScheduleController 단위 테스트")
class AdminScheduleControllerTest extends BaseAdminControllerWebMvcTest {

	@Nested
	@DisplayName("영업 스케줄 조회")
	class GetSchedules {

		@Test
		@DisplayName("음식점 스케줄을 조회하면 주간 스케줄을 반환한다")
		void 스케줄_조회_성공() throws Exception {
			// given
			var item = BusinessHourWeekItem.from(
				LocalDate.parse("2026-02-01"),
				DayOfWeekCode.MON,
				false,
				LocalTime.parse("09:00"),
				LocalTime.parse("22:00"),
				ScheduleSource.WEEKLY,
				null);
			given(restaurantScheduleService.getBusinessHoursWeek(1L))
				.willReturn(List.of(item));

			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants/1/schedules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].dayOfWeek").value("MON"))
				.andExpect(jsonPath("$.data[0].openTime").value("09:00"))
				.andExpect(jsonPath("$.data[0].source").value("WEEKLY"));
		}

		@Test
		@DisplayName("레스토랑 ID가 정수가 아니면 400으로 실패한다")
		void 스케줄_조회_레스토랑_ID_타입_실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants/abc/schedules"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}

		@Test
		@DisplayName("음식점이 없으면 404로 실패한다")
		void 스케줄_조회_음식점_미존재_실패() throws Exception {
			// given
			given(restaurantScheduleService.getBusinessHoursWeek(999L))
				.willThrow(new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/api/v1/admin/restaurants/999/schedules"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESTAURANT_NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("영업 스케줄 등록")
	class CreateSchedules {

		@Test
		@DisplayName("유효한 스케줄 목록이면 생성하고 201을 반환한다")
		void 스케줄_생성_성공() throws Exception {
			// given
			List<WeeklyScheduleRequest> request = List.of(
				new WeeklyScheduleRequest(
					1,
					LocalTime.parse("09:00"),
					LocalTime.parse("18:00"),
					false,
					LocalDate.parse("2026-02-01"),
					LocalDate.parse("2026-02-07")));
			doNothing().when(restaurantScheduleService).createWeeklySchedules(1L, request);

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/schedules")
				.contentType(APPLICATION_JSON)
				.content("""
					[
					  {
					    "dayOfWeek": 1,
					    "openTime": "09:00",
					    "closeTime": "18:00",
					    "isClosed": false,
					    "effectiveFrom": "2026-02-01",
					    "effectiveTo": "2026-02-07"
					  }
					]
					"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true));
		}

		@Test
		@DisplayName("요청 스키마가 잘못되면 내부 처리 오류로 실패한다")
		void 스케줄_생성_요청형식_실패() throws Exception {
			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/1/schedules")
				.contentType(APPLICATION_JSON)
				.content("not-json"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}

		@Test
		@DisplayName("음식점이 없으면 404로 실패한다")
		void 스케줄_생성_음식점_미존재_실패() throws Exception {
			// given
			List<WeeklyScheduleRequest> request = List.of(
				new WeeklyScheduleRequest(
					1,
					LocalTime.parse("09:00"),
					LocalTime.parse("18:00"),
					false,
					LocalDate.parse("2026-02-01"),
					LocalDate.parse("2026-02-07")));
			doThrow(new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND))
				.when(restaurantScheduleService)
				.createWeeklySchedules(999L, request);

			// when & then
			mockMvc.perform(post("/api/v1/admin/restaurants/999/schedules")
				.contentType(APPLICATION_JSON)
				.content("""
					[
					  {
					    "dayOfWeek": 1,
					    "openTime": "09:00",
					    "closeTime": "18:00",
					    "isClosed": false,
					    "effectiveFrom": "2026-02-01",
					    "effectiveTo": "2026-02-07"
					  }
					]
					"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESTAURANT_NOT_FOUND"));
		}
	}
}
