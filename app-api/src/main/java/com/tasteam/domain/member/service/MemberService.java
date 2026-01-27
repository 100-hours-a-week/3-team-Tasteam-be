package com.tasteam.domain.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final GroupMemberRepository groupMemberRepository;

	@Transactional(readOnly = true)
	public MemberMeResponse getMyProfile(Long memberId) {
		Member member = getActiveMember(memberId);
		return MemberMeResponse.from(member);
	}

	@Transactional(readOnly = true)
	public List<MemberGroupSummaryResponse> getMyGroupSummaries(Long memberId) {
		// 멤버 존재 여부를 먼저 확인해 404를 일관되게 유지합니다.
		getActiveMember(memberId);
		return groupMemberRepository.findMemberGroupSummaries(memberId, GroupStatus.ACTIVE);
	}

	@Transactional
	public void updateMyProfile(Long memberId, MemberProfileUpdateRequest request) {
		Member member = getActiveMember(memberId);

		if (request.email() != null && !request.email().equals(member.getEmail())) {
			validateEmailDuplication(memberId, request.email());
			member.changeEmail(request.email());
		}

		if (request.profileImageUrl() != null
			&& !request.profileImageUrl().equals(member.getProfileImageUrl())) {
			member.changeProfileImageUrl(request.profileImageUrl());
		}
	}

	@Transactional
	public void withdraw(Long memberId) {
		Member member = getActiveMember(memberId);
		member.withdraw();
	}

	private Member getActiveMember(Long memberId) {
		return memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private void validateEmailDuplication(Long memberId, String email) {
		if (memberRepository.existsByEmailAndIdNot(email, memberId)) {
			throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
		}
	}
}
