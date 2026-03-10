package com.tasteam.domain.recommendation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModel;
import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModelStatus;

public interface RestaurantRecommendationModelRepository extends JpaRepository<RestaurantRecommendationModel, Long> {

	Optional<RestaurantRecommendationModel> findByStatus(RestaurantRecommendationModelStatus status);

	Optional<RestaurantRecommendationModel> findByVersion(String version);

	boolean existsByVersion(String version);

	List<RestaurantRecommendationModel> findAllByOrderByCreatedAtDesc();
}
