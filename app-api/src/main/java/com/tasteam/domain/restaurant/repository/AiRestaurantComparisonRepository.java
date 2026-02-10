package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.AiRestaurantComparison;

public interface AiRestaurantComparisonRepository extends JpaRepository<AiRestaurantComparison, Long> {

	Optional<AiRestaurantComparison> findByRestaurantId(Long restaurantId);
}
