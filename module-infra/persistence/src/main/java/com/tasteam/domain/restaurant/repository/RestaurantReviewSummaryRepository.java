package com.tasteam.domain.restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;

public interface RestaurantReviewSummaryRepository extends JpaRepository<RestaurantReviewSummary, Long> {

	/**
	 * restaurant_id당 1건 (갱신 방식). 배치 upsert·조회용.
	 */
	Optional<RestaurantReviewSummary> findByRestaurantId(Long restaurantId);

	/**
	 * restaurant_id 목록에 대해 조회. 음식점당 1건이므로 목록 길이 ≤ restaurantIds 크기.
	 */
	List<RestaurantReviewSummary> findByRestaurantIdIn(List<Long> restaurantIds);

	/**
	 * 해당 restaurant_id + vector_epoch 결과 존재 여부. RUNNING Job COMPLETED 보정용.
	 */
	boolean existsByRestaurantIdAndVectorEpoch(Long restaurantId, long vectorEpoch);
}
