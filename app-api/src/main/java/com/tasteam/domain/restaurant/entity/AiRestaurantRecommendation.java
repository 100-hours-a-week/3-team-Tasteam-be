package com.tasteam.domain.restaurant.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "ai_restaurant_recommendation")
@Comment("Vector 검색 추천 결과를 TTL 기반으로 저장하는 캐시 테이블")
public class AiRestaurantRecommendation extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(name = "reason", nullable = false, length = 1000)
	@Comment("빈 문자열 불가")
	private String reason;

	@Column(name = "cache_key", nullable = false, length = 120)
	@Comment("추천 캐시 식별자 (예: memberId + query)")
	private String cacheKey;

	@Column(name = "expires_at", nullable = false)
	@Comment("캐시 만료 시각 (기본 생성시각 + 1일)")
	private Instant expiresAt;

	public static AiRestaurantRecommendation create(
		Restaurant restaurant,
		String reason,
		String cacheKey,
		Instant expiresAt) {
		return AiRestaurantRecommendation.builder()
			.restaurant(restaurant)
			.reason(reason)
			.cacheKey(cacheKey)
			.expiresAt(expiresAt)
			.build();
	}
}
