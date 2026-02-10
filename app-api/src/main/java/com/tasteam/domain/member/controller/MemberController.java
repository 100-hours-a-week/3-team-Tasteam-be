package com.tasteam.domain.member.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.favorite.dto.request.FavoriteCreateRequest;
import com.tasteam.domain.favorite.dto.response.FavoriteCreateResponse;
import com.tasteam.domain.favorite.dto.response.FavoritePageTargetsResponse;
import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.dto.response.FavoriteTargetsResponse;
import com.tasteam.domain.favorite.dto.response.SubgroupFavoriteRestaurantItem;
import com.tasteam.domain.favorite.service.FavoriteService;
import com.tasteam.domain.member.controller.docs.MemberControllerDocs;
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupDetailSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.dto.response.ReviewSummaryResponse;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.service.SubgroupFacade;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

	private final MemberService memberService;
	private final SubgroupFacade subgroupFacade;
	private final ReviewService reviewService;
	private final FavoriteService favoriteService;

	@GetMapping
	public SuccessResponse<MemberMeResponse> getMyMemberInfo(
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(memberService.getMyProfile(memberId));
	}

	@GetMapping("/groups/summary")
	public SuccessResponse<List<MemberGroupSummaryResponse>> getMyGroupSummaries(
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(memberService.getMyGroupSummaries(memberId));
	}

	@GetMapping("/groups")
	public SuccessResponse<List<MemberGroupDetailSummaryResponse>> getMyGroups(
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(memberService.getMyGroupDetails(memberId));
	}

	@GetMapping("/groups/{groupId}/subgroups")
	public SuccessResponse<SubgroupListResponse> getMySubgroups(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestParam(required = false)
		String keyword,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(subgroupFacade.getMySubgroups(groupId, memberId, keyword, cursor, size));
	}

	@PatchMapping("/profile")
	public SuccessResponse<Void> updateMyProfile(
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		MemberProfileUpdateRequest request) {
		memberService.updateMyProfile(memberId, request);
		return SuccessResponse.success();
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/favorites/restaurants")
	public SuccessResponse<CursorPageResponse<FavoriteRestaurantItem>> getMyFavoriteRestaurants(
		@CurrentUser
		Long memberId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(favoriteService.getMyFavoriteRestaurants(memberId, request.cursor()));
	}

	@PreAuthorize("hasRole('USER')")
	@PostMapping("/favorites/restaurants")
	public SuccessResponse<FavoriteCreateResponse> createMyFavoriteRestaurant(
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		FavoriteCreateRequest request) {
		return SuccessResponse.success(favoriteService.createMyFavorite(memberId, request.restaurantId()));
	}

	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/favorites/restaurants/{restaurantId}")
	public ResponseEntity<Void> deleteMyFavoriteRestaurant(
		@CurrentUser
		Long memberId,
		@PathVariable @Positive
		Long restaurantId) {
		favoriteService.deleteMyFavorite(memberId, restaurantId);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/favorite-targets")
	public SuccessResponse<FavoritePageTargetsResponse> getFavoriteTargets(
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(favoriteService.getFavoriteTargets(memberId));
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/restaurants/{restaurantId}/favorite-targets")
	public SuccessResponse<FavoriteTargetsResponse> getRestaurantFavoriteTargets(
		@CurrentUser
		Long memberId,
		@PathVariable @Positive
		Long restaurantId) {
		return SuccessResponse.success(favoriteService.getFavoriteTargets(memberId, restaurantId));
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/subgroups/{subgroupId}/favorites/restaurants")
	public SuccessResponse<CursorPageResponse<SubgroupFavoriteRestaurantItem>> getSubgroupFavoriteRestaurants(
		@CurrentUser
		Long memberId,
		@PathVariable @Positive
		Long subgroupId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(favoriteService.getSubgroupFavoriteRestaurants(memberId, subgroupId,
			request.cursor()));
	}

	@PreAuthorize("hasRole('USER')")
	@PostMapping("/subgroups/{subgroupId}/favorites/restaurants")
	public SuccessResponse<FavoriteCreateResponse> createSubgroupFavoriteRestaurant(
		@CurrentUser
		Long memberId,
		@PathVariable @Positive
		Long subgroupId,
		@RequestBody @Validated
		FavoriteCreateRequest request) {
		return SuccessResponse.success(favoriteService.createSubgroupFavorite(memberId, subgroupId,
			request.restaurantId()));
	}

	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/subgroups/{subgroupId}/favorites/restaurants/{restaurantId}")
	public ResponseEntity<Void> deleteSubgroupFavoriteRestaurant(
		@CurrentUser
		Long memberId,
		@PathVariable @Positive
		Long subgroupId,
		@PathVariable @Positive
		Long restaurantId) {
		favoriteService.deleteSubgroupFavorite(memberId, subgroupId, restaurantId);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/reviews")
	public SuccessResponse<CursorPageResponse<ReviewSummaryResponse>> getMyReviews(
		@CurrentUser
		Long memberId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(reviewService.getMemberReviews(memberId, request));
	}

	@DeleteMapping
	public SuccessResponse<Void> withdraw(
		@CurrentUser
		Long memberId) {
		memberService.withdraw(memberId);
		return SuccessResponse.success();
	}
}
