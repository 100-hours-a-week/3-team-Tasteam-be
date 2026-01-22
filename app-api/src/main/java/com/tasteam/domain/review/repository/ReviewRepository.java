package com.tasteam.domain.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	long countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull(Long restaurantId);

	long countByRestaurantIdAndIsRecommendedFalseAndDeletedAtIsNull(Long restaurantId);

	List<Review> findByRestaurantIdAndDeletedAtIsNull(long restaurantId);
}
