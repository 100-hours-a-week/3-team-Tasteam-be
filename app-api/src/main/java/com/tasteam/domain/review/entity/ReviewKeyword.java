package com.tasteam.domain.review.entity;

import org.hibernate.annotations.Comment;
import org.springframework.util.Assert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "review_keyword", uniqueConstraints = {
	@UniqueConstraint(name = "uq_review_keyword", columnNames = {"review_id", "keyword_id"})
})
@Comment("리뷰-키워드 매핑")
public class ReviewKeyword {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id", nullable = false)
	private Keyword keyword;

	public static ReviewKeyword create(Review review, Keyword keyword) {
		validateCreate(review, keyword);
		return ReviewKeyword.builder()
			.review(review)
			.keyword(keyword)
			.build();
	}

	private static void validateCreate(Review review, Keyword keyword) {
		Assert.notNull(review, "리뷰는 필수입니다");
		Assert.notNull(keyword, "키워드는 필수입니다");
	}
}
