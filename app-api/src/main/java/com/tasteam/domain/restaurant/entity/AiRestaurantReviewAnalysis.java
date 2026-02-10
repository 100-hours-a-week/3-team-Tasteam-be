package com.tasteam.domain.restaurant.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tasteam.domain.common.BaseTimeEntity;
import com.tasteam.domain.restaurant.type.AnalysisStatus;

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
@Table(name = "ai_restaurant_review_analysis")
@Comment("음식점별 AI 리뷰 분석 스냅샷(1행)을 저장하는 테이블")
public class AiRestaurantReviewAnalysis extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "restaurant_id", nullable = false, unique = true)
	private Long restaurantId;

	@Column(name = "overall_summary", nullable = false, length = 1000)
	private String overallSummary;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "category_summaries", nullable = false, columnDefinition = "jsonb")
	@Comment("카테고리별 요약. 예) service/price/food")
	private Map<String, String> categorySummaries;

	@Column(name = "positive_ratio", nullable = false, precision = 5, scale = 4)
	@Comment("긍정 비율 (0.0000 ~ 1.0000)")
	private BigDecimal positiveRatio;

	@Column(name = "negative_ratio", nullable = false, precision = 5, scale = 4)
	@Comment("부정 비율 (0.0000 ~ 1.0000)")
	private BigDecimal negativeRatio;

	@Column(name = "analyzed_at")
	@Comment("마지막 분석 완료 시점")
	private Instant analyzedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private AnalysisStatus status;

	public static AiRestaurantReviewAnalysis create(
		Long restaurantId,
		String overallSummary,
		Map<String, String> categorySummaries,
		BigDecimal positiveRatio,
		BigDecimal negativeRatio,
		Instant analyzedAt,
		AnalysisStatus status) {
		return AiRestaurantReviewAnalysis.builder()
			.restaurantId(restaurantId)
			.overallSummary(overallSummary)
			.categorySummaries(categorySummaries)
			.positiveRatio(positiveRatio)
			.negativeRatio(negativeRatio)
			.analyzedAt(analyzedAt)
			.status(status)
			.build();
	}

	public void markAnalyzing() {
		status = AnalysisStatus.ANALYZING;
	}

	public void markCompleted() {
		status = AnalysisStatus.COMPLETED;
	}

	public void updateAnalysis(
		String overallSummary,
		Map<String, String> categorySummaries,
		BigDecimal positiveRatio,
		BigDecimal negativeRatio,
		Instant analyzedAt) {
		this.overallSummary = Objects.requireNonNullElse(overallSummary, "");
		this.categorySummaries = categorySummaries == null ? Map.of() : new HashMap<>(categorySummaries);
		this.positiveRatio = normalizeRatio(positiveRatio);
		this.negativeRatio = normalizeRatio(negativeRatio);
		this.analyzedAt = analyzedAt;
		this.status = AnalysisStatus.COMPLETED;
	}

	public static AiRestaurantReviewAnalysis create(Long restaurantId, String overallSummary,
		BigDecimal positiveRatio) {
		BigDecimal boundedPositive = positiveRatio == null
			? BigDecimal.ZERO
			: positiveRatio.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
		BigDecimal calculatedNegative = BigDecimal.ONE.subtract(boundedPositive).setScale(4, RoundingMode.HALF_UP);
		return create(
			restaurantId,
			overallSummary,
			Map.of(),
			boundedPositive,
			calculatedNegative,
			Instant.now(),
			AnalysisStatus.COMPLETED);
	}

	public static AiRestaurantReviewAnalysis createEmpty(Long restaurantId, AnalysisStatus status) {
		return create(
			restaurantId,
			"",
			Map.of(),
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			null,
			status);
	}

	@Deprecated
	public String getSummary() {
		return overallSummary;
	}

	@Deprecated
	public BigDecimal getPositiveReviewRatio() {
		return positiveRatio;
	}

	private BigDecimal normalizeRatio(BigDecimal value) {
		return (value == null ? BigDecimal.ZERO : value)
			.max(BigDecimal.ZERO)
			.min(BigDecimal.ONE)
			.setScale(4, RoundingMode.HALF_UP);
	}
}
