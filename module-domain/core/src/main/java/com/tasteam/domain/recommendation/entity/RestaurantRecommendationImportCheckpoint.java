package com.tasteam.domain.recommendation.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "restaurant_recommendation_import_checkpoint")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantRecommendationImportCheckpoint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "pipeline_version", nullable = false, length = 100)
	private String pipelineVersion;

	@Column(name = "batch_dt", nullable = false)
	private LocalDate batchDt;

	@Column(name = "imported_at", nullable = false)
	private Instant importedAt;

	public static RestaurantRecommendationImportCheckpoint of(String pipelineVersion, LocalDate batchDt,
		Instant importedAt) {
		RestaurantRecommendationImportCheckpoint checkpoint = new RestaurantRecommendationImportCheckpoint();
		checkpoint.pipelineVersion = pipelineVersion;
		checkpoint.batchDt = batchDt;
		checkpoint.importedAt = importedAt;
		return checkpoint;
	}
}
