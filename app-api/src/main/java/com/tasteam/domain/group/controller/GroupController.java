package com.tasteam.domain.group.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.group.controller.docs.GroupControllerDocs;
import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationRequest;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationRequest;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationRequest;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.group.service.GroupService;
import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/groups")
@RequiredArgsConstructor
@Validated
public class GroupController implements GroupControllerDocs {

	private final GroupService groupService;
	private final RestaurantService restaurantService;
	private final ReviewService reviewService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupCreateResponse> createGroup(@RequestBody @Validated
	GroupCreateRequest request) {
		return SuccessResponse.success(groupService.createGroup(request));
	}

	@GetMapping("/{groupId}")
	public SuccessResponse<GroupGetResponse> getGroup(@PathVariable @Positive
	Long groupId) {
		return SuccessResponse.success(groupService.getGroup(groupId));
	}

	@PatchMapping("/{groupId}")
	public SuccessResponse<Void> updateGroup(
		@PathVariable @Positive
		Long groupId,
		@RequestBody @Validated
		GroupUpdateRequest request) {
		groupService.updateGroup(groupId, request);
		return SuccessResponse.success(null);
	}

	@DeleteMapping("/{groupId}")
	public SuccessResponse<Void> deleteGroup(@PathVariable @Positive
	Long groupId) {
		groupService.deleteGroup(groupId);
		return SuccessResponse.success(null);
	}

	@DeleteMapping("/{groupId}/members/me")
	public SuccessResponse<Void> withdrawGroup(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId) {
		groupService.withdrawGroup(groupId, memberId);
		return SuccessResponse.success(null);
	}

	@PostMapping("/{groupId}/email-verifications")
	public SuccessResponse<GroupEmailVerificationResponse> sendGroupEmailVerification(
		@PathVariable @Positive
		Long groupId,
		@RequestBody @Validated
		GroupEmailVerificationRequest request) {
		return SuccessResponse.success(groupService.sendGroupEmailVerification(groupId, request.email()));
	}

	@PostMapping("/{groupId}/email-authentications")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupEmailAuthenticationResponse> authenticateGroupByEmail(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		GroupEmailAuthenticationRequest request) {
		return SuccessResponse.success(
			groupService.authenticateGroupByEmail(groupId, memberId, request.code()));
	}

	@PostMapping("/{groupId}/password-authentications")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupPasswordAuthenticationResponse> authenticateGroupByPassword(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		GroupPasswordAuthenticationRequest request) {
		return SuccessResponse.success(
			groupService.authenticateGroupByPassword(groupId, memberId, request.code()));
	}

	@GetMapping("/{groupId}/members")
	public SuccessResponse<GroupMemberListResponse> getGroupMembers(
		@PathVariable @Positive
		Long groupId,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(groupService.getGroupMembers(groupId, cursor, size));
	}

	@DeleteMapping("/{groupId}/members/{userId}")
	public SuccessResponse<Void> deleteGroupMember(
		@PathVariable @Positive
		Long groupId,
		@PathVariable @Positive
		Long userId) {
		groupService.deleteGroupMember(groupId, userId);
		return SuccessResponse.success(null);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{groupId}/reviews")
	public SuccessResponse<CursorPageResponse<ReviewResponse>> getGroupReviews(
		@PathVariable @Positive
		Long groupId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(reviewService.getGroupReviews(groupId, request));
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{groupId}/reviews/restaurants")
	public CursorPageResponse<RestaurantListItem> getGroupReviewRestaurants(
		@PathVariable @Positive
		Long groupId,
		@ModelAttribute @Validated
		NearbyRestaurantQueryParams queryParams) {
		return restaurantService.getGroupRestaurants(groupId, queryParams);
	}
}
