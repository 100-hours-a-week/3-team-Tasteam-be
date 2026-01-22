package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;

public interface AiRestaurantReviewAnalysisRepository extends JpaRepository<AiRestaurantReviewAnalysis, Long> {

	Optional<AiRestaurantReviewAnalysis> findByRestaurantId(Long restaurantId);
}
