package com.tasteam.domain.member.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupDetailSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberGroupDetailSummaryRow;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryRow;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.domain.member.dto.response.MemberSubgroupDetailSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberSubgroupDetailSummaryRow;
import com.tasteam.domain.member.dto.response.MemberSubgroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberSubgroupSummaryRow;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.type.SubgroupStatus;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final SubgroupMemberRepository subgroupMemberRepository;
	private final FileService fileService;

	@Transactional(readOnly = true)
	public MemberMeResponse getMyProfile(Long memberId) {
		Member member = getActiveMember(memberId);
		String profileImageUrl = resolveProfileImageUrl(member);
		return MemberMeResponse.from(member, profileImageUrl);
	}

	@Transactional(readOnly = true)
	public List<MemberGroupSummaryResponse> getMyGroupSummaries(Long memberId) {
		// 멤버 존재 여부를 먼저 확인해 404를 일관되게 유지합니다.
		getActiveMember(memberId);
		List<MemberGroupSummaryRow> groupRows = groupMemberRepository.findMemberGroupSummaries(
			memberId,
			GroupStatus.ACTIVE);
		List<MemberSubgroupSummaryRow> subgroupRows = subgroupMemberRepository.findMemberSubgroupSummaries(
			memberId,
			SubgroupStatus.ACTIVE,
			GroupStatus.ACTIVE);
		Map<Long, MemberGroupSummaryResponse> grouped = new LinkedHashMap<>();
		for (MemberGroupSummaryRow row : groupRows) {
			MemberGroupSummaryResponse summary = grouped.get(row.groupId());
			if (summary == null) {
				summary = new MemberGroupSummaryResponse(
					row.groupId(),
					row.groupName(),
					new ArrayList<>());
				grouped.put(row.groupId(), summary);
			}
		}
		for (MemberSubgroupSummaryRow row : subgroupRows) {
			MemberGroupSummaryResponse summary = grouped.get(row.groupId());
			if (summary != null) {
				summary.subGroups().add(new MemberSubgroupSummaryResponse(
					row.subGroupId(),
					row.subGroupName()));
			}
		}
		return new ArrayList<>(grouped.values());
	}

	@Transactional(readOnly = true)
	public List<MemberGroupDetailSummaryResponse> getMyGroupDetails(Long memberId) {
		getActiveMember(memberId);
		List<MemberGroupDetailSummaryRow> groupRows = groupMemberRepository.findMemberGroupDetailSummaries(
			memberId,
			GroupStatus.ACTIVE);
		List<MemberSubgroupDetailSummaryRow> subgroupRows = subgroupMemberRepository.findMemberSubgroupDetailSummaries(
			memberId,
			SubgroupStatus.ACTIVE,
			GroupStatus.ACTIVE);

		Map<Long, String> groupImageUrlById = resolveDomainImageUrlMap(
			DomainType.GROUP,
			groupRows.stream().map(MemberGroupDetailSummaryRow::groupId).toList());
		Map<Long, String> subgroupImageUrlById = resolveDomainImageUrlMap(
			DomainType.SUBGROUP,
			subgroupRows.stream().map(MemberSubgroupDetailSummaryRow::subGroupId).toList());

		Map<Long, MemberGroupDetailSummaryResponse> grouped = new LinkedHashMap<>();
		for (MemberGroupDetailSummaryRow row : groupRows) {
			MemberGroupDetailSummaryResponse summary = new MemberGroupDetailSummaryResponse(
				row.groupId(),
				row.groupName(),
				row.groupAddress(),
				row.groupDetailAddress(),
				groupImageUrlById.get(row.groupId()),
				row.groupMemberCount(),
				new ArrayList<>());
			grouped.put(row.groupId(), summary);
		}
		for (MemberSubgroupDetailSummaryRow row : subgroupRows) {
			MemberGroupDetailSummaryResponse summary = grouped.get(row.groupId());
			if (summary != null) {
				summary.subGroups().add(new MemberSubgroupDetailSummaryResponse(
					row.subGroupId(),
					row.subGroupName(),
					row.memberCount(),
					subgroupImageUrlById.get(row.subGroupId())));
			}
		}
		return new ArrayList<>(grouped.values());
	}

	@Transactional
	public void updateMyProfile(Long memberId, MemberProfileUpdateRequest request) {
		Member member = getActiveMember(memberId);

		if (request.email() != null && !request.email().equals(member.getEmail())) {
			validateEmailDuplication(memberId, request.email());
			member.changeEmail(request.email());
		}

		if (request.nickname() != null && !request.nickname().equals(member.getNickname())) {
			member.changeNickname(request.nickname());
		}

		if (request.introduction() != null && !request.introduction().equals(member.getIntroduction())) {
			member.changeIntroduction(request.introduction());
		}

		if (request.profileImageFileUuid() != null) {
			fileService.replaceDomainImage(DomainType.MEMBER, memberId, request.profileImageFileUuid());
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

	private String resolveProfileImageUrl(Member member) {
		return fileService.getPrimaryDomainImageUrl(DomainType.MEMBER, member.getId());
	}

	private Map<Long, String> resolveDomainImageUrlMap(DomainType domainType, List<Long> domainIds) {
		if (domainIds.isEmpty()) {
			return Map.of();
		}
		return fileService.getPrimaryDomainImageUrlMap(domainType, domainIds);
	}
}
