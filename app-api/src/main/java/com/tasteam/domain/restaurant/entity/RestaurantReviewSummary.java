package com.tasteam.domain.restaurant.entity;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "restaurant_review_summary")
@Comment("음식점별 리뷰 요약 분석 결과 (restaurant_id당 1건, 갱신 방식)")
public class RestaurantReviewSummary {

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

	@Getter(AccessLevel.NONE)
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "summary_json", nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> summaryJson;

	@Column(name = "analyzed_at", nullable = false)
	private Instant analyzedAt;

	/**
	 * 읽기 전용. 반환된 Map에 put/remove 등 수정 연산을 하면 UnsupportedOperationException 발생.
	 * 값을 변경하지 말 것.
	 */
	public Map<String, Object> getSummaryJson() {
		return summaryJson == null
			? Map.of()
			: Collections.unmodifiableMap(new HashMap<>(summaryJson));
	}

	/**
	 * 요약 분석 Job 완료 시 저장용. 배치(12번)에서 호출.
	 */
	public static RestaurantReviewSummary create(
		Long restaurantId, long vectorEpoch, String modelVersion,
		Map<String, Object> summaryJson, Instant analyzedAt) {
		return RestaurantReviewSummary.builder()
			.restaurantId(restaurantId)
			.vectorEpoch(vectorEpoch)
			.modelVersion(modelVersion)
			.summaryJson(summaryJson != null ? new HashMap<>(summaryJson) : new HashMap<>())
			.analyzedAt(analyzedAt)
			.build();
	}

	/**
	 * 기존 행이 있을 때 요약 결과만 갱신 (restaurant_id당 1건 갱신 방식).
	 */
	public void update(Map<String, Object> summaryJson, long vectorEpoch, Instant analyzedAt) {
		this.summaryJson = summaryJson != null ? new HashMap<>(summaryJson) : new HashMap<>();
		this.vectorEpoch = vectorEpoch;
		this.analyzedAt = analyzedAt;
	}
}
