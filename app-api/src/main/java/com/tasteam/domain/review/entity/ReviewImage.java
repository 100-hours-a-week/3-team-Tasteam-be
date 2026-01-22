package com.tasteam.domain.review.entity;

import org.hibernate.annotations.Comment;
import org.springframework.util.Assert;

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
@Table(name = "review_image")
@Comment("리뷰 이미지")
public class ReviewImage extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;

	public static ReviewImage create(Review review, String imageUrl) {
		validateCreate(review, imageUrl);
		return ReviewImage.builder()
			.review(review)
			.imageUrl(imageUrl)
			.build();
	}

	private static void validateCreate(Review review, String imageUrl) {
		Assert.notNull(review, "리뷰는 필수입니다");
		Assert.hasText(imageUrl, "이미지 URL은 필수입니다");
		if (imageUrl.length() > 500) {
			throw new IllegalArgumentException("이미지 URL이 너무 깁니다");
		}
	}
}
