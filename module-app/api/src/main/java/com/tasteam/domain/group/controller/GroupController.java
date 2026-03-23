package com.tasteam.domain.group.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.tasteam.domain.group.service.GroupFacade;
import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.service.SubgroupFacade;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.ratelimit.ClientIpResolver;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/groups")
@RequiredArgsConstructor
@Validated
public class GroupController implements GroupControllerDocs {

	private final GroupFacade groupFacade;
	private final RestaurantService restaurantService;
	private final ReviewService reviewService;
	private final SubgroupFacade subgroupFacade;
	private final ClientIpResolver clientIpResolver;

	@PostMapping
	@PreAuthorize("hasRole('USER')")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupCreateResponse> createGroup(@RequestBody @Validated
	GroupCreateRequest request) {
		return SuccessResponse.success(groupFacade.createGroup(request));
	}

	@GetMapping("/{groupId}")
	public SuccessResponse<GroupGetResponse> getGroup(@PathVariable @Positive
	Long groupId) {
		return SuccessResponse.success(groupFacade.getGroup(groupId));
	}

	@PatchMapping("/{groupId}")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<Void> updateGroup(
		@PathVariable @Positive
		Long groupId,
		@RequestBody @Validated
		GroupUpdateRequest request) {
		groupFacade.updateGroup(groupId, request);
		return SuccessResponse.success(null);
	}

	@DeleteMapping("/{groupId}")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<Void> deleteGroup(@PathVariable @Positive
	Long groupId) {
		groupFacade.deleteGroup(groupId);
		return SuccessResponse.success(null);
	}

	@DeleteMapping("/{groupId}/members/me")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<Void> withdrawGroup(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId) {
		groupFacade.withdrawGroup(groupId, memberId);
		return SuccessResponse.success(null);
	}

	@PostMapping("/{groupId}/email-verifications")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<GroupEmailVerificationResponse> sendGroupEmailVerification(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		HttpServletRequest servletRequest,
		@RequestBody @Validated
		GroupEmailVerificationRequest request) {
		String clientIp = clientIpResolver.resolve(servletRequest);
		return SuccessResponse
			.success(groupFacade.sendGroupEmailVerification(groupId, memberId, clientIp, request.email()));
	}

	@PostMapping("/{groupId}/email-authentications")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<GroupEmailAuthenticationResponse> authenticateGroupByEmail(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		GroupEmailAuthenticationRequest request) {
		return SuccessResponse.success(groupFacade.authenticateGroupByEmail(groupId, memberId, request.token()));
	}

	@GetMapping("/{groupId}/email-authentications")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<GroupEmailAuthenticationResponse> authenticateGroupByEmailByLink(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestParam
		String token) {
		return SuccessResponse.success(groupFacade.authenticateGroupByEmail(groupId, memberId, token));
	}

	@PostMapping("/{groupId}/password-authentications")
	@PreAuthorize("hasRole('USER')")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupPasswordAuthenticationResponse> authenticateGroupByPassword(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		GroupPasswordAuthenticationRequest request) {
		return SuccessResponse.success(
			groupFacade.authenticateGroupByPassword(groupId, memberId, request.code()));
	}

	@GetMapping("/{groupId}/members")
	public SuccessResponse<GroupMemberListResponse> getGroupMembers(
		@PathVariable @Positive
		Long groupId,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(groupFacade.getGroupMembers(groupId, cursor, size));
	}

	@DeleteMapping("/{groupId}/members/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public SuccessResponse<Void> deleteGroupMember(
		@PathVariable @Positive
		Long groupId,
		@PathVariable @Positive
		Long userId) {
		groupFacade.deleteGroupMember(groupId, userId);
		return SuccessResponse.success(null);
	}

	@GetMapping("/{groupId}/subgroups")
	public SuccessResponse<CursorPageResponse<SubgroupListItem>> getGroupSubgroups(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(subgroupFacade.getGroupSubgroups(groupId, memberId, cursor, size));
	}

	@GetMapping("/{groupId}/subgroups/search")
	public SuccessResponse<CursorPageResponse<SubgroupListItem>> searchSubgroups(
		@PathVariable @Positive
		Long groupId,
		@RequestParam(required = false)
		String keyword,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(subgroupFacade.searchGroupSubgroups(groupId, keyword, cursor, size));
	}

	@PostMapping("/{groupId}/subgroups")
	@PreAuthorize("hasRole('USER')")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<SubgroupCreateResponse> createSubgroup(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		SubgroupCreateRequest request) {
		return SuccessResponse.success(subgroupFacade.createSubgroup(groupId, memberId, request));
	}

	@PostMapping("/{groupId}/subgroups/{subgroupId}/members")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<SubgroupJoinResponse> joinSubgroup(
		@PathVariable @Positive
		Long groupId,
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId,
		@RequestBody(required = false)
		SubgroupJoinRequest request) {
		return SuccessResponse.success(subgroupFacade.joinSubgroup(groupId, subgroupId, memberId, request));
	}

	@PatchMapping("/{groupId}/subgroups/{subgroupId}")
	@PreAuthorize("hasRole('USER')")
	public SuccessResponse<Void> updateSubgroup(
		@PathVariable @Positive
		Long groupId,
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		SubgroupUpdateRequest request) {
		subgroupFacade.updateSubgroup(groupId, subgroupId, memberId, request);
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
	public SuccessResponse<CursorPageResponse<RestaurantListItem>> getGroupReviewRestaurants(
		@PathVariable @Positive
		Long groupId,
		@ModelAttribute @Validated
		NearbyRestaurantQueryParams queryParams) {
		return SuccessResponse.success(restaurantService.getGroupRestaurants(groupId, queryParams));
	}
}
