package com.tasteam.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;
import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.RestaurantWeeklyScheduleRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

@ServiceIntegrationTest
@Transactional
class RestaurantScheduleServiceIntegrationTest {

	@Autowired
	private RestaurantScheduleService restaurantScheduleService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private RestaurantWeeklyScheduleRepository weeklyScheduleRepository;

	@Nested
	@DisplayName("주간 스케줄 생성")
	class CreateWeeklySchedules {

		@Test
		@DisplayName("주간 스케줄이 저장되고 조회 결과에 반영된다")
		void createWeeklySchedulesSuccess() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("스케줄 음식점"));
			List<WeeklyScheduleRequest> schedules = IntStream.rangeClosed(1, 7)
				.mapToObj(day -> new WeeklyScheduleRequest(
					day, LocalTime.of(9, 0), LocalTime.of(22, 0), false, null, null))
				.toList();

			restaurantScheduleService.createWeeklySchedules(restaurant.getId(), schedules);

			assertThat(weeklyScheduleRepository.findByRestaurantId(restaurant.getId())).hasSize(7);

			List<BusinessHourWeekItem> week = restaurantScheduleService.getBusinessHoursWeek(restaurant.getId());
			assertThat(week).hasSize(7);
			assertThat(week).allMatch(item -> "WEEKLY".equals(item.source()));
		}

		@Test
		@DisplayName("존재하지 않는 음식점이면 실패한다")
		void createWeeklySchedulesNotFoundFails() {
			List<WeeklyScheduleRequest> schedules = List.of(
				new WeeklyScheduleRequest(1, LocalTime.of(9, 0), LocalTime.of(22, 0), false, null, null));

			assertThatThrownBy(() -> restaurantScheduleService.createWeeklySchedules(999999L, schedules))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name());
		}
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울특별시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-3333-4444");
	}
}
