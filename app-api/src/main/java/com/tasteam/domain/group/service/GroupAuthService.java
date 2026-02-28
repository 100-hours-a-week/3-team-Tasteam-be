package com.tasteam.domain.group.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupAuthCode;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.event.GroupEventPublisher;
import com.tasteam.domain.group.repository.GroupAuthCodeRepository;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.service.GroupInviteTokenService.InviteClaims;
import com.tasteam.domain.group.service.GroupInviteTokenService.InviteToken;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.config.DomainProperties;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.exception.code.NotificationErrorCode;
import com.tasteam.global.notification.email.EmailSender;
import com.tasteam.global.ratelimit.RateLimitReason;
import com.tasteam.global.ratelimit.RateLimitRequest;
import com.tasteam.global.ratelimit.RateLimitResult;
import com.tasteam.global.ratelimit.RedisRateLimiter;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupAuthService {

	private static final DateTimeFormatter EXPIRES_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
		.withZone(ZoneId.of("Asia/Seoul"));

	private final GroupAuthCodeRepository groupAuthCodeRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailSender emailSender;
	private final GroupEventPublisher groupEventPublisher;
	private final GroupInviteTokenService groupInviteTokenService;
	private final DomainProperties domainProperties;
	private final RedisRateLimiter redisRateLimiter;
	private final MeterRegistry meterRegistry;

	@Transactional
	public void saveInitialPasswordCode(Group group, String code) {
		if (group.getJoinType() != GroupJoinType.PASSWORD) {
			return;
		}
		groupAuthCodeRepository.save(GroupAuthCode.builder()
			.groupId(group.getId())
			.code(passwordEncoder.encode(code))
			.email(null)
			.expiresAt(null)
			.build());
	}

	@Transactional
	public GroupEmailVerificationResponse sendGroupEmailVerification(Group group, Long memberId, String clientIp,
		String email) {
		if (group.getJoinType() != GroupJoinType.EMAIL || group.getEmailDomain() == null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		validateEmailDomain(email, group.getEmailDomain());
		enforceMailRateLimit(memberId, clientIp, email);

		InviteToken inviteToken = groupInviteTokenService.issue(group.getId(), email);
		String verificationUrl = buildVerificationUrl(group.getId(), inviteToken.token());
		emailSender.sendTemplateEmail(email, "group-join-verification", Map.of(
			"subject", "[Tasteam] 그룹 가입 이메일 인증",
			"verificationUrl", verificationUrl,
			"expiresAt", EXPIRES_AT_FORMATTER.format(inviteToken.expiresAt())));
		return new GroupEmailVerificationResponse(inviteToken.expiresAt());
	}

	private void enforceMailRateLimit(Long memberId, String clientIp, String email) {
		meterRegistry.counter("mail_send_request_count").increment();
		RateLimitResult result = redisRateLimiter.checkMailSend(new RateLimitRequest(email, clientIp, memberId));
		if (result.allowed()) {
			return;
		}
		RateLimitReason reason = result.reason();
		meterRegistry.counter("rate_limited_count", "reason", reason.name()).increment();

		if (reason == RateLimitReason.EMAIL_BLOCKED_24H) {
			throw new BusinessException(NotificationErrorCode.EMAIL_BLOCKED_24H);
		}
		if (reason == RateLimitReason.RATE_LIMITER_UNAVAILABLE) {
			throw new BusinessException(NotificationErrorCode.EMAIL_RATE_LIMITER_UNAVAILABLE);
		}
		throw new BusinessException(NotificationErrorCode.EMAIL_RATE_LIMITED);
	}

	@Transactional
	public GroupEmailAuthenticationResponse authenticateGroupByEmail(Group group, Long memberId, String token) {
		if (group.getJoinType() != GroupJoinType.EMAIL) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		InviteClaims claims = groupInviteTokenService.parse(token);
		if (!group.getId().equals(claims.groupId())) {
			throw new BusinessException(GroupErrorCode.EMAIL_TOKEN_INVALID);
		}

		var member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		if (!StringUtils.hasText(member.getEmail()) || !member.getEmail().equalsIgnoreCase(claims.email())) {
			throw new BusinessException(GroupErrorCode.EMAIL_TOKEN_INVALID);
		}

		GroupMember groupMember = groupMemberRepository
			.findByGroupIdAndMember_Id(group.getId(), member.getId())
			.orElse(null);

		if (groupMember == null) {
			groupMember = groupMemberRepository.save(GroupMember.create(
				group.getId(),
				member));
		} else if (groupMember.getDeletedAt() != null) {
			groupMember.restore();
		} else {
			return new GroupEmailAuthenticationResponse(true, groupMember.getCreatedAt());
		}

		groupEventPublisher.publishMemberJoined(group.getId(), member.getId(), group.getName(),
			groupMember.getCreatedAt());
		return new GroupEmailAuthenticationResponse(true, groupMember.getCreatedAt());
	}

	@Transactional
	public GroupPasswordAuthenticationResponse authenticateGroupByPassword(Group group, Long memberId, String code) {
		if (group.getJoinType() != GroupJoinType.PASSWORD || group.getType() != GroupType.UNOFFICIAL
			|| group.getStatus() != GroupStatus.ACTIVE) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		GroupAuthCode authCode = groupAuthCodeRepository.findByGroupId(group.getId())
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_PASSWORD_MISMATCH));

		if (!passwordEncoder.matches(code, authCode.getCode())) {
			throw new BusinessException(GroupErrorCode.GROUP_PASSWORD_MISMATCH);
		}

		GroupMember groupMember = groupMemberRepository
			.findByGroupIdAndMember_Id(group.getId(), memberId)
			.orElse(null);
		if (groupMember == null) {
			groupMember = groupMemberRepository.save(GroupMember.create(
				group.getId(),
				memberRepository.findByIdAndDeletedAtIsNull(memberId)
					.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND))));
		} else if (groupMember.getDeletedAt() != null) {
			groupMember.restore();
		}

		groupEventPublisher.publishMemberJoined(group.getId(), memberId, group.getName(), groupMember.getCreatedAt());
		return new GroupPasswordAuthenticationResponse(true, groupMember.getCreatedAt());
	}

	private void validateEmailDomain(String email, String domain) {
		int atIndex = email.lastIndexOf('@');
		if (atIndex < 0 || atIndex == email.length() - 1) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		String emailDomain = email.substring(atIndex + 1);
		if (!emailDomain.equalsIgnoreCase(domain)) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private String buildVerificationUrl(Long groupId, String token) {
		if (!StringUtils.hasText(domainProperties.getService())) {
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
		String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
		return domainProperties.getService() + "/api/v1/groups/" + groupId + "/email-authentications?token="
			+ encodedToken;
	}
}
