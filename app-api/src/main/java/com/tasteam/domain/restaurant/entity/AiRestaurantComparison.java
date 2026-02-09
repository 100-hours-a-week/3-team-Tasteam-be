package com.tasteam.domain.restaurant.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
@Table(name = "ai_restaurant_comparison")
@Comment("음식점별 AI 비교 분석 스냅샷(1행)을 저장하는 테이블")
public class AiRestaurantComparison extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "restaurant_id", nullable = false, unique = true)
	private Long restaurantId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "category_lift", nullable = false, columnDefinition = "jsonb")
	@Comment("카테고리별 lift 비율. 예) service/price/food")
	private Map<String, BigDecimal> categoryLift;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "comparison_display", nullable = false, columnDefinition = "jsonb")
	@Comment("비교 문장 목록")
	private List<String> comparisonDisplay;

	@Column(name = "total_candidates", nullable = false)
	private Integer totalCandidates;

	@Column(name = "validated_count", nullable = false)
	private Integer validatedCount;

	@Column(name = "analyzed_at")
	@Comment("마지막 분석 완료 시점")
	private Instant analyzedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private AnalysisStatus status;

	public static AiRestaurantComparison create(
		Long restaurantId,
		Map<String, BigDecimal> categoryLift,
		List<String> comparisonDisplay,
		Integer totalCandidates,
		Integer validatedCount,
		Instant analyzedAt,
		AnalysisStatus status) {
		return AiRestaurantComparison.builder()
			.restaurantId(restaurantId)
			.categoryLift(categoryLift)
			.comparisonDisplay(comparisonDisplay)
			.totalCandidates(totalCandidates)
			.validatedCount(validatedCount)
			.analyzedAt(analyzedAt)
			.status(status)
			.build();
	}

	public static AiRestaurantComparison create(Restaurant restaurant, String comparisonSentence) {
		return AiRestaurantComparison.builder()
			.restaurantId(restaurant.getId())
			.categoryLift(Map.of())
			.comparisonDisplay(List.of(comparisonSentence))
			.totalCandidates(0)
			.validatedCount(0)
			.analyzedAt(Instant.now())
			.status(AnalysisStatus.COMPLETED)
			.build();
	}

	@Deprecated
	public String getContent() {
		return comparisonDisplay == null || comparisonDisplay.isEmpty() ? null : comparisonDisplay.getFirst();
	}
}
