package com.tasteam.domain.restaurant.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.RestaurantWeeklySchedule;

public interface RestaurantWeeklyScheduleRepository extends JpaRepository<RestaurantWeeklySchedule, Long> {

	@Query("""
		SELECT rws FROM RestaurantWeeklySchedule rws
		WHERE rws.restaurant.id = :restaurantId
		  AND rws.dayOfWeek IN :dayOfWeeks
		  AND (rws.effectiveFrom IS NULL OR rws.effectiveFrom <= :targetDate)
		  AND (rws.effectiveTo IS NULL OR rws.effectiveTo >= :targetDate)
		ORDER BY rws.dayOfWeek ASC, rws.effectiveFrom DESC NULLS LAST
		""")
	List<RestaurantWeeklySchedule> findEffectiveSchedules(
		@Param("restaurantId")
		Long restaurantId,
		@Param("dayOfWeeks")
		Set<Integer> dayOfWeeks,
		@Param("targetDate")
		LocalDate targetDate);

	List<RestaurantWeeklySchedule> findByRestaurantId(Long restaurantId);
}
