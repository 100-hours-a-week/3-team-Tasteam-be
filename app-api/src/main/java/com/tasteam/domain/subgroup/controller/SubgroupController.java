package com.tasteam.domain.subgroup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.controller.docs.SubgroupControllerDocs;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/subgroups")
@RequiredArgsConstructor
@Validated
public class SubgroupController implements SubgroupControllerDocs {

	private final ReviewService reviewService;

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{subgroupId}/reviews")
	public SuccessResponse<CursorPageResponse<ReviewResponse>> getSubgroupReviews(
		@PathVariable @Positive
		Long subgroupId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(reviewService.getSubgroupReviews(subgroupId, request));
	}
}
