package com.tasteam.domain.group.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupAuthCode;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.event.GroupEventPublisher;
import com.tasteam.domain.group.repository.GroupAuthCodeRepository;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.group.type.GroupType;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.notification.email.EmailSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupAuthService {

	private static final DateTimeFormatter KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		.withZone(ZoneId.of("Asia/Seoul"));

	private final GroupAuthCodeRepository groupAuthCodeRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailSender emailSender;
	private final GroupEventPublisher groupEventPublisher;

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
	public GroupEmailVerificationResponse sendGroupEmailVerification(Group group, String email) {
		if (group.getJoinType() != GroupJoinType.EMAIL || group.getEmailDomain() == null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		validateEmailDomain(email, group.getEmailDomain());
		Instant now = Instant.now();
		if (groupAuthCodeRepository.existsByGroupIdAndExpiresAtAfterAndVerifiedAtIsNull(group.getId(), now)) {
			throw new BusinessException(GroupErrorCode.EMAIL_ALREADY_EXISTS);
		}

		String code = generateVerificationCode();
		Instant expiresAt = now.plus(Duration.ofMinutes(10));
		GroupAuthCode authCode = groupAuthCodeRepository.save(GroupAuthCode.builder()
			.groupId(group.getId())
			.code(code)
			.email(email)
			.expiresAt(expiresAt)
			.build());

		emailSender.sendTemplateEmail(email, "group-join-verification", Map.of(
			"code", code,
			"expiresAt", KST_FORMATTER.format(expiresAt),
			"subject", "[Tasteam] 그룹 참여 인증 코드"));
		return GroupEmailVerificationResponse.from(authCode);
	}

	@Transactional
	public GroupEmailAuthenticationResponse authenticateGroupByEmail(Group group, Long memberId, String code) {
		if (group.getJoinType() != GroupJoinType.EMAIL) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		GroupAuthCode authCode = groupAuthCodeRepository
			.findByGroupIdAndCodeAndExpiresAtAfterAndVerifiedAtIsNull(group.getId(), code, Instant.now())
			.orElseThrow(() -> new BusinessException(GroupErrorCode.EMAIL_CODE_MISMATCH));

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

		authCode.verify(Instant.now());
		groupEventPublisher.publishMemberJoined(group.getId(), memberId, group.getName(), groupMember.getCreatedAt());

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

	private String generateVerificationCode() {
		int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
		return String.valueOf(code);
	}
}
