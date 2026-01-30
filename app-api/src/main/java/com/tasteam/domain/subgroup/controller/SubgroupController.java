package com.tasteam.domain.subgroup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.controller.docs.SubgroupControllerDocs;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.service.SubgroupService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/subgroups")
@RequiredArgsConstructor
@Validated
public class SubgroupController implements SubgroupControllerDocs {

	private final ReviewService reviewService;
	private final SubgroupService subgroupService;

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{subgroupId}/reviews")
	public SuccessResponse<CursorPageResponse<ReviewResponse>> getSubgroupReviews(
		@PathVariable @Positive
		Long subgroupId,
		@ModelAttribute @Validated
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(reviewService.getSubgroupReviews(subgroupId, request));
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{subgroupId}/members")
	public SuccessResponse<CursorPageResponse<SubgroupMemberListItem>> getSubgroupMembers(
		@PathVariable @Positive
		Long subgroupId,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(subgroupService.getSubgroupMembers(subgroupId, cursor, size));
	}

	@GetMapping("/{subgroupId}")
	public SuccessResponse<SubgroupDetailResponse> getSubgroup(
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(subgroupService.getSubgroup(subgroupId, memberId));
	}

	@DeleteMapping("/{subgroupId}/members/me")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<Void> withdrawSubgroup(
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId) {
		subgroupService.withdrawSubgroup(subgroupId, memberId);
		return ResponseEntity.noContent().build();
	}
}
