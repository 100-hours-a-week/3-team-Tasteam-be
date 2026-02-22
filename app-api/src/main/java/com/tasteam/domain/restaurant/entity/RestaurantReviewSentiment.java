package com.tasteam.domain.restaurant.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "restaurant_review_sentiment")
@Comment("음식점별 리뷰 감정 분석 결과 (restaurant_id, vector_epoch 기준 1건)")
public class RestaurantReviewSentiment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Column(name = "vector_epoch", nullable = false)
	private long vectorEpoch;

	@Column(name = "model_version", length = 50)
	private String modelVersion;

	@Column(name = "positive_count", nullable = false)
	private int positiveCount;

	@Column(name = "negative_count", nullable = false)
	private int negativeCount;

	@Column(name = "neutral_count", nullable = false)
	private int neutralCount;

	@Column(name = "positive_ratio", nullable = false, precision = 5, scale = 4)
	private BigDecimal positiveRatio;

	@Column(name = "negative_ratio", nullable = false, precision = 5, scale = 4)
	private BigDecimal negativeRatio;

	@Column(name = "neutral_ratio", nullable = false, precision = 5, scale = 4)
	private BigDecimal neutralRatio;

	@Column(name = "analyzed_at", nullable = false)
	private Instant analyzedAt;

	/**
	 * 감정 분석 Job 완료 시 저장용. 배치(11번)에서 호출.
	 */
	public static RestaurantReviewSentiment create(
		Long restaurantId, long vectorEpoch, String modelVersion,
		int positiveCount, int negativeCount, int neutralCount,
		BigDecimal positiveRatio, BigDecimal negativeRatio, BigDecimal neutralRatio,
		Instant analyzedAt) {
		return RestaurantReviewSentiment.builder()
			.restaurantId(restaurantId)
			.vectorEpoch(vectorEpoch)
			.modelVersion(modelVersion)
			.positiveCount(positiveCount)
			.negativeCount(negativeCount)
			.neutralCount(neutralCount)
			.positiveRatio(positiveRatio)
			.negativeRatio(negativeRatio)
			.neutralRatio(neutralRatio)
			.analyzedAt(analyzedAt)
			.build();
	}
}
