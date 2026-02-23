package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.RestaurantReviewSentiment;

public interface RestaurantReviewSentimentRepository extends JpaRepository<RestaurantReviewSentiment, Long> {

	/**
	 * restaurant_id 기준 최신 1건 (vector_epoch 최대). 조회 전환(16번)에서 사용.
	 */
	Optional<RestaurantReviewSentiment> findTopByRestaurantIdOrderByVectorEpochDesc(Long restaurantId);

	/**
	 * restaurant_id + vector_epoch 기준 1건 조회. UPSERT 시 기존 건 확인용.
	 */
	Optional<RestaurantReviewSentiment> findByRestaurantIdAndVectorEpoch(Long restaurantId, long vectorEpoch);
}
