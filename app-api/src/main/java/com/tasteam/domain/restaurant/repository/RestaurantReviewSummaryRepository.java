package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;

public interface RestaurantReviewSummaryRepository extends JpaRepository<RestaurantReviewSummary, Long> {

	/**
	 * restaurant_id 기준 최신 1건 (vector_epoch 최대). 조회 전환(16번)에서 사용.
	 */
	Optional<RestaurantReviewSummary> findTopByRestaurantIdOrderByVectorEpochDesc(Long restaurantId);
}
