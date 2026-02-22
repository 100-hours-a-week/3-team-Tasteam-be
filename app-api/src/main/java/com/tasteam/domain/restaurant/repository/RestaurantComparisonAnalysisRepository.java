package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.RestaurantComparisonAnalysis;

public interface RestaurantComparisonAnalysisRepository extends JpaRepository<RestaurantComparisonAnalysis, Long> {

	/**
	 * restaurant_id당 1건 (유니크). 조회 전환(16번)에서 사용.
	 */
	Optional<RestaurantComparisonAnalysis> findByRestaurantId(Long restaurantId);
}
