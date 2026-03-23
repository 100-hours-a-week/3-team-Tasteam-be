package com.tasteam.domain.restaurant.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.restaurant.entity.RestaurantScheduleOverride;

public interface RestaurantScheduleOverrideRepository extends JpaRepository<RestaurantScheduleOverride, Long> {

	@Query("""
		SELECT rso FROM RestaurantScheduleOverride rso
		WHERE rso.restaurant.id = :restaurantId
		  AND rso.date BETWEEN :startDate AND :endDate
		""")
	List<RestaurantScheduleOverride> findByRestaurantIdAndDateRange(
		@Param("restaurantId")
		Long restaurantId,
		@Param("startDate")
		LocalDate startDate,
		@Param("endDate")
		LocalDate endDate);

	Optional<RestaurantScheduleOverride> findByRestaurantIdAndDate(Long restaurantId, LocalDate date);
}
