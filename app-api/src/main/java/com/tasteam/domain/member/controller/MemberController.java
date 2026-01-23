package com.tasteam.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.member.controller.docs.MemberControllerDocs;
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.dto.response.MemberPreviewResponse;
import com.tasteam.domain.member.dto.response.ReviewPreviewResponse;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

	private final MemberService memberService;
	private final ReviewService reviewService;

	@GetMapping
	public SuccessResponse<MemberMeResponse> getMyMemberInfo(
		@RequestHeader("X-Member-Id")
		Long memberId) {
		return SuccessResponse.success(memberService.getMyProfile(memberId));
	}

	@PatchMapping("/profile")
	public ResponseEntity<Void> updateMyProfile(
		@RequestHeader("X-Member-Id")
		Long memberId,
		@Valid @RequestBody
		MemberProfileUpdateRequest request) {
		memberService.updateMyProfile(memberId, request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	public ResponseEntity<Void> withdraw(
		@RequestHeader("X-Member-Id")
		Long memberId) {
		memberService.withdraw(memberId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/reviews")
	public SuccessResponse<MemberPreviewResponse<ReviewPreviewResponse>> getMyReviews(
		@RequestHeader("X-Member-Id")
		Long memberId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(reviewService.getMemberReviews(memberId, request));
	}
}
