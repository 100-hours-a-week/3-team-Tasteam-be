package com.tasteam.domain.test.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.test.controller.docs.WebhookTestControllerDocs;
import com.tasteam.domain.test.dto.DevMemberResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class WebhookTestController implements WebhookTestControllerDocs {

	private static final Long DEV_MEMBER_ID = 1001L;

	private final MemberRepository memberRepository;
	private final MemberService memberService;

	@GetMapping("/error/business")
	public String testBusinessException() {
		throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
	}

	@GetMapping("/error/system")
	public String testSystemException() {
		throw new RuntimeException("테스트용 시스템 예외 발생");
	}

	@GetMapping("/dev/member")
	public SuccessResponse<DevMemberResponse> getDevMember() {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(DEV_MEMBER_ID)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		List<MemberGroupSummaryResponse> groups = memberService.getMyGroupSummaries(DEV_MEMBER_ID);
		return SuccessResponse.success(new DevMemberResponse(
			member.getId(),
			member.getEmail(),
			member.getNickname(),
			member.getProfileImageUrl(),
			groups));
	}
}
