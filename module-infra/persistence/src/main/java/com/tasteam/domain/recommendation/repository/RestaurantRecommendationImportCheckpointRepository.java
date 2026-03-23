package com.tasteam.domain.recommendation.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.recommendation.entity.RestaurantRecommendationImportCheckpoint;

public interface RestaurantRecommendationImportCheckpointRepository
	extends JpaRepository<RestaurantRecommendationImportCheckpoint, Long> {

	boolean existsByPipelineVersionAndBatchDt(String pipelineVersion, LocalDate batchDt);
}
