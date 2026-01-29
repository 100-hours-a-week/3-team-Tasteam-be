package com.tasteam.domain.restaurant.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseTimeEntity;

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
@Table(name = "ai_restaurant_review_analysis")
@Comment("AI가 음식점의 리뷰 데이터를 분석하여 생성한 요약 및 긍정 비율 정보를 저장하는 테이블")
public class AiRestaurantReviewAnalysis extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "restaurant_id", nullable = false, unique = true)
	private Long restaurantId;

	@Column(name = "summary", nullable = false, length = 500)
	@Comment("빈 문자열 불가")
	private String summary;

	@Column(name = "positive_review_ratio", nullable = false, precision = 3, scale = 2)
	@Comment("긍정 리뷰 비율 (0.00 ~ 1.00)")
	private BigDecimal positiveReviewRatio;

	public static AiRestaurantReviewAnalysis create(Long restaurantId, String summary,
		BigDecimal positiveReviewRatio) {
		return AiRestaurantReviewAnalysis.builder()
			.restaurantId(restaurantId)
			.summary(summary)
			.positiveReviewRatio(positiveReviewRatio)
			.build();
	}
}
