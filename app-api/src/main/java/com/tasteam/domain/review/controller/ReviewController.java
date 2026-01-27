package com.tasteam.domain.review.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.review.dto.response.ReviewDetailResponse;
import com.tasteam.domain.review.dto.response.ReviewKeywordItemResponse;
import com.tasteam.domain.review.entity.KeywordType;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {

	private final ReviewService reviewService;

	@GetMapping("/keywords")
	public SuccessResponse<List<ReviewKeywordItemResponse>> getReviewKeywords(
		@RequestParam(required = false)
		KeywordType type) {
		return SuccessResponse.success(reviewService.getReviewKeywords(type));
	}

	@GetMapping("/{reviewId}")
	public SuccessResponse<ReviewDetailResponse> getReview(
		@PathVariable @Positive
		Long reviewId) {
		return SuccessResponse.success(reviewService.getReviewDetail(reviewId));
	}

	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/{reviewId}")
	public ResponseEntity<Void> deleteReview(
		@PathVariable @Positive
		Long reviewId,
		@CurrentUser
		Long memberId) {
		reviewService.deleteReview(memberId, reviewId);
		return ResponseEntity.noContent().build();
	}
}
