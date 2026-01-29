package com.tasteam.domain.member.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.member.controller.docs.MemberControllerDocs;
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

	private final MemberService memberService;

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

	@PatchMapping("/profile")
	public SuccessResponse<Void> updateMyProfile(
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		MemberProfileUpdateRequest request) {
		memberService.updateMyProfile(memberId, request);
		return SuccessResponse.success();
	}

	@DeleteMapping
	public SuccessResponse<Void> withdraw(
		@CurrentUser
		Long memberId) {
		memberService.withdraw(memberId);
		return SuccessResponse.success();
	}
}
