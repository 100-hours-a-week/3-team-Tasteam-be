package com.tasteam.domain.review.entity;

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

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static ReviewImage create(Review review, String imageUrl) {
		return ReviewImage.builder()
			.review(review)
			.imageUrl(imageUrl)
			.deletedAt(null)
			.build();
	}

	public void softDelete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
