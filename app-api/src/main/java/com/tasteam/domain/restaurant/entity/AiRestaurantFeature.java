package com.tasteam.domain.restaurant.entity;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "ai_restaurant_feature")
@Comment("AI 분석을 통해 도출된 음식점의 특징을 설명하는 글을 저장하는 테이블")
public class AiRestaurantFeature extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false, unique = true)
	private Restaurant restaurant;

	@Column(name = "content", nullable = false, length = 500)
	@Comment("빈 문자열 불가")
	private String content;

	public static AiRestaurantFeature create(Restaurant restaurant, String content) {
		return AiRestaurantFeature.builder()
			.restaurant(restaurant)
			.content(content)
			.build();
	}
}
