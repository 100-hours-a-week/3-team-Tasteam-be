package com.tasteam.domain.recommendation.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant_recommendation_model")
public class RestaurantRecommendationModel extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "version", nullable = false, length = 100, unique = true)
	private String version;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private RestaurantRecommendationModelStatus status;

	@Column(name = "activated_at")
	private Instant activatedAt;

	@Column(name = "deactivated_at")
	private Instant deactivatedAt;

	public static RestaurantRecommendationModel loading(String version) {
		return RestaurantRecommendationModel.builder()
			.version(version)
			.status(RestaurantRecommendationModelStatus.LOADING)
			.activatedAt(null)
			.deactivatedAt(null)
			.build();
	}

	public void markReady() {
		this.status = RestaurantRecommendationModelStatus.READY;
	}

	public void markLoading() {
		this.status = RestaurantRecommendationModelStatus.LOADING;
	}

	public void markFailed() {
		this.status = RestaurantRecommendationModelStatus.FAILED;
	}

	public void markActive(Instant activatedAt) {
		this.status = RestaurantRecommendationModelStatus.ACTIVE;
		this.activatedAt = activatedAt;
		this.deactivatedAt = null;
	}

	public void markInactive(Instant deactivatedAt) {
		this.status = RestaurantRecommendationModelStatus.INACTIVE;
		this.deactivatedAt = deactivatedAt;
	}
}
