package com.tasteam.domain.restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.restaurant.entity.AiRestaurantFeature;

public interface AiRestaurantFeatureRepository extends JpaRepository<AiRestaurantFeature, Long> {

	Optional<AiRestaurantFeature> findByRestaurantId(Long restaurantId);
}
