package com.tasteam.domain.restaurant.entity;

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
import lombok.Setter;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant_review_sentiment")
@Comment("음식점별 리뷰 감정 분석 결과 (restaurant_id당 1건, 갱신 방식)")
public class RestaurantReviewSentiment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Setter
	@Column(name = "vector_epoch", nullable = false)
	private long vectorEpoch;

	@Column(name = "model_version", length = 50)
	private String modelVersion;

	@Setter
	@Column(name = "positive_count", nullable = false)
	private int positiveCount;

	@Setter
	@Column(name = "negative_count", nullable = false)
	private int negativeCount;

	@Setter
	@Column(name = "neutral_count", nullable = false)
	private int neutralCount;

	@Setter
	@Column(name = "positive_percent", nullable = false)
	private Short positivePercent;

	@Setter
	@Column(name = "negative_percent", nullable = false)
	private Short negativePercent;

	@Setter
	@Column(name = "neutral_percent", nullable = false)
	private Short neutralPercent;

	@Setter
	@Column(name = "analyzed_at", nullable = false)
	private Instant analyzedAt;

	/**
	 * 기존 행이 있을 때 감정 분석 결과만 갱신 (restaurant_id당 1건 갱신 방식).
	 * ratio는 0–100.
	 */
	public void update(
		long vectorEpoch,
		int positiveCount, int negativeCount, int neutralCount,
		int positiveRatio, int negativeRatio, int neutralRatio,
		Instant analyzedAt) {
		this.vectorEpoch = vectorEpoch;
		this.positiveCount = positiveCount;
		this.negativeCount = negativeCount;
		this.neutralCount = neutralCount;
		this.positivePercent = (short)positiveRatio;
		this.negativePercent = (short)negativeRatio;
		this.neutralPercent = (short)neutralRatio;
		this.analyzedAt = analyzedAt;
	}

	/**
	 * 감정 분석 Job 완료 시 저장용. 배치(11번)에서 호출.
	 * percent는 0–100.
	 */
	public static RestaurantReviewSentiment create(
		Long restaurantId, long vectorEpoch, String modelVersion,
		int positiveCount, int negativeCount, int neutralCount,
		int positivePercent, int negativePercent, int neutralPercent,
		Instant analyzedAt) {
		return RestaurantReviewSentiment.builder()
			.restaurantId(restaurantId)
			.vectorEpoch(vectorEpoch)
			.modelVersion(modelVersion)
			.positiveCount(positiveCount)
			.negativeCount(negativeCount)
			.neutralCount(neutralCount)
			.positivePercent((short)positivePercent)
			.negativePercent((short)negativePercent)
			.neutralPercent((short)neutralPercent)
			.analyzedAt(analyzedAt)
			.build();
	}
}
