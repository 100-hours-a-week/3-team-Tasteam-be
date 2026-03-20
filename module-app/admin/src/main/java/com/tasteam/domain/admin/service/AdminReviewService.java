package com.tasteam.domain.admin.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.response.AdminReviewListItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.ReviewKeywordRepository;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.ReviewErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

	private final ReviewRepository reviewRepository;
	private final ReviewKeywordRepository reviewKeywordRepository;
	private final DomainImageRepository domainImageRepository;

	@Transactional(readOnly = true)
	public Page<AdminReviewListItem> getReviews(Long restaurantId, Pageable pageable) {
		Page<Review> reviews = (restaurantId != null)
			? reviewRepository.findAllByRestaurant_IdAndDeletedAtIsNull(restaurantId, pageable)
			: reviewRepository.findAllByDeletedAtIsNull(pageable);

		return reviews.map(r -> new AdminReviewListItem(
			r.getId(),
			r.getRestaurant().getId(),
			r.getRestaurant().getName(),
			r.getMember().getId(),
			r.getMember().getNickname(),
			r.getContent(),
			r.isRecommended(),
			r.getCreatedAt()));
	}

	@Transactional
	public void deleteReview(Long reviewId) {
		Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
			.orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));

		review.softDelete(Instant.now());
		reviewKeywordRepository.deleteByReview_Id(reviewId);
		domainImageRepository.deleteAllByDomainTypeAndDomainId(DomainType.REVIEW, reviewId);
	}
}
