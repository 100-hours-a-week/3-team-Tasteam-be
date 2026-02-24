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
@Table(name = "restaurant_comparison")
@Comment("음식점별 비교 분석 결과 (restaurant_id당 1건)")
public class RestaurantComparison {

	private static final String DEFAULT_MODEL_VERSION = "1";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Column(name = "model_version", length = 50)
	private String modelVersion;

	@Getter(AccessLevel.NONE)
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "comparison_json", nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> comparisonJson;

	@Column(name = "analyzed_at", nullable = false)
	private Instant analyzedAt;

	/**
	 * 읽기 전용. 반환된 Map에 put/remove 등 수정 연산을 하면 UnsupportedOperationException 발생.
	 * 값을 변경하지 말 것.
	 */
	public Map<String, Object> getComparisonJson() {
		return comparisonJson == null
			? Map.of()
			: Collections.unmodifiableMap(new HashMap<>(comparisonJson));
	}

	/**
	 * 비교 분석 Job 완료 시 저장용. 배치에서 호출.
	 */
	public static RestaurantComparison create(
		Long restaurantId, String modelVersion,
		Map<String, Object> comparisonJson, Instant analyzedAt) {
		return RestaurantComparison.builder()
			.restaurantId(restaurantId)
			.modelVersion(modelVersion != null ? modelVersion : DEFAULT_MODEL_VERSION)
			.comparisonJson(comparisonJson != null ? new HashMap<>(comparisonJson) : new HashMap<>())
			.analyzedAt(analyzedAt)
			.build();
	}

	/**
	 * 기존 행이 있을 때 비교 결과만 갱신. (restaurant_id 유니크 기준 upsert)
	 */
	public void updateResult(Map<String, Object> comparisonJson, Instant analyzedAt) {
		this.comparisonJson = comparisonJson != null ? new HashMap<>(comparisonJson) : new HashMap<>();
		this.analyzedAt = analyzedAt;
	}
}
