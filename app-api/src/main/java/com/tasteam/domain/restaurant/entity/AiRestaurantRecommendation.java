package com.tasteam.domain.restaurant.entity;

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
@Comment("AI 분석을 통해 생성된 음식점 추천 결과를 배치 단위로 누적 저장하는 테이블")
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

	public static AiRestaurantRecommendation create(Restaurant restaurant, String reason) {
		return AiRestaurantRecommendation.builder()
			.restaurant(restaurant)
			.reason(reason)
			.build();
	}
}
