package com.tasteam.domain.test.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.member.service.MemberService;
import com.tasteam.domain.test.controller.docs.WebhookTestControllerDocs;
import com.tasteam.domain.test.dto.DevMemberResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class WebhookTestController implements WebhookTestControllerDocs {

	private static final Long DEV_MEMBER_ID = 1001L;
	private static final long ONE_HOUR_MS = 3600000L;

	private final MemberRepository memberRepository;
	private final MemberService memberService;
	private final com.tasteam.global.security.jwt.provider.JwtTokenProvider jwtTokenProvider;
	private final JwtCookieProvider jwtCookieProvider;
	private final RefreshTokenStore refreshTokenStore;

	@GetMapping("/error/business")
	public String testBusinessException() {
		throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
	}

	@GetMapping("/error/system")
	public String testSystemException() {
		throw new RuntimeException("테스트용 시스템 예외 발생");
	}

	@GetMapping("/dev/member")
	public SuccessResponse<DevMemberResponse> getDevMember(HttpServletResponse response) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(DEV_MEMBER_ID)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		List<MemberGroupSummaryResponse> groups = memberService.getMyGroupSummaries(DEV_MEMBER_ID);

		String accessToken = jwtTokenProvider.generateAccessToken(
			member.getId(), member.getRole().name(), ONE_HOUR_MS);
		String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

		jwtCookieProvider.addRefreshTokenCookie(response, refreshToken);

		String tokenFamilyId = UUID.randomUUID().toString();
		refreshTokenStore.revokeByMemberId(member.getId(), Instant.now());
		refreshTokenStore.save(RefreshToken.issue(
			member.getId(),
			RefreshTokenHasher.hash(refreshToken),
			tokenFamilyId,
			jwtTokenProvider.getExpiration(refreshToken).toInstant()));

		return SuccessResponse.success(new DevMemberResponse(
			member.getId(),
			member.getEmail(),
			member.getNickname(),
			member.getProfileImageUrl(),
			groups,
			accessToken));
	}
}
