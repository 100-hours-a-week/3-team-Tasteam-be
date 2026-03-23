package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.RestaurantReviewSentiment;

public interface RestaurantReviewSentimentRepository extends JpaRepository<RestaurantReviewSentiment, Long> {

	/**
	 * restaurant_id당 1건 (갱신 방식). 배치 upsert·조회용.
	 */
	Optional<RestaurantReviewSentiment> findByRestaurantId(Long restaurantId);

	/**
	 * 해당 restaurant_id + vector_epoch 결과 존재 여부. RUNNING Job COMPLETED 보정용.
	 */
	boolean existsByRestaurantIdAndVectorEpoch(Long restaurantId, long vectorEpoch);
}
