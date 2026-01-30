package com.tasteam.domain.member.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.service.FavoriteService;
import com.tasteam.domain.member.controller.docs.MemberControllerDocs;
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.dto.response.ReviewSummaryResponse;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.service.SubgroupService;
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
	private final SubgroupService subgroupService;
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
		return SuccessResponse.success(subgroupService.getMySubgroups(groupId, memberId, keyword, cursor, size));
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
