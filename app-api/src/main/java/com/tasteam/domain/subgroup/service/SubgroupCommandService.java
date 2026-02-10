package com.tasteam.domain.subgroup.service;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.domain.subgroup.type.SubgroupStatus;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.exception.code.SubgroupErrorCode;
import com.tasteam.global.utils.JsonNodePatchUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubgroupCommandService {

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final MemberRepository memberRepository;
	private final SubgroupRepository subgroupRepository;
	private final SubgroupMemberRepository subgroupMemberRepository;
	private final PasswordEncoder passwordEncoder;
	private final FileService fileService;

	@Transactional
	public SubgroupCreateResponse createSubgroup(Long groupId, Long memberId, SubgroupCreateRequest request) {
		validateAuthenticated(memberId);
		Group group = getGroup(groupId);
		if (group.getStatus() != GroupStatus.ACTIVE) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		validateGroupMember(group.getId(), memberId);
		validateJoinType(request.getJoinType(), request.getPassword());

		if (subgroupRepository.existsByGroup_IdAndNameAndDeletedAtIsNull(groupId, request.getName())) {
			throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
		}

		String encodedPassword = null;
		if (request.getJoinType() == SubgroupJoinType.PASSWORD) {
			encodedPassword = passwordEncoder.encode(request.getPassword());
		}

		Subgroup subgroup = Subgroup.builder()
			.group(group)
			.name(request.getName())
			.description(request.getDescription())
			.joinType(request.getJoinType())
			.joinPassword(encodedPassword)
			.status(SubgroupStatus.ACTIVE)
			.memberCount(1)
			.build();

		try {
			subgroupRepository.save(subgroup);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
		}

		if (request.getProfileImageFileUuid() != null) {
			applySubgroupImage(subgroup, request.getProfileImageFileUuid());
		}

		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		subgroupMemberRepository.save(SubgroupMember.create(subgroup.getId(), member));

		return SubgroupCreateResponse.from(subgroup);
	}

	@Transactional
	public SubgroupJoinResponse joinSubgroup(Long groupId, Long subgroupId, Long memberId,
		SubgroupJoinRequest request) {
		validateAuthenticated(memberId);
		getGroup(groupId);

		Subgroup subgroup = subgroupRepository.findByIdAndGroup_IdAndStatusAndDeletedAtIsNull(
			subgroupId,
			groupId,
			SubgroupStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND));

		validateGroupMember(groupId, memberId);
		validateJoinRequest(subgroup, request);

		SubgroupMember subgroupMember = subgroupMemberRepository.findBySubgroupIdAndMember_Id(subgroupId, memberId)
			.orElse(null);

		Instant joinedAt;
		if (subgroupMember == null) {
			Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
				.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
			subgroupMember = subgroupMemberRepository.save(SubgroupMember.create(subgroupId, member));
			subgroup.increaseMemberCount();
			joinedAt = subgroupMember.getCreatedAt();
		} else if (subgroupMember.getDeletedAt() != null) {
			subgroupMember.restore();
			subgroup.increaseMemberCount();
			joinedAt = subgroupMember.getCreatedAt();
		} else {
			throw new BusinessException(SubgroupErrorCode.SUBGROUP_ALREADY_JOINED);
		}

		return new SubgroupJoinResponse(
			new SubgroupJoinResponse.JoinData(
				subgroupId,
				joinedAt));
	}

	@Transactional
	public void withdrawSubgroup(Long subgroupId, Long memberId) {
		validateAuthenticated(memberId);
		Subgroup subgroup = subgroupRepository.findByIdAndDeletedAtIsNull(subgroupId)
			.orElseThrow(() -> new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND));

		SubgroupMember subgroupMember = subgroupMemberRepository.findBySubgroupIdAndMember_Id(subgroupId, memberId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NO_PERMISSION));

		if (subgroupMember.getDeletedAt() != null) {
			return;
		}
		subgroupMember.softDelete(Instant.now());
		subgroup.decreaseMemberCount();
	}

	@Transactional
	public void updateSubgroup(Long groupId, Long subgroupId, Long memberId, SubgroupUpdateRequest request) {
		validateAuthenticated(memberId);
		getGroup(groupId);
		if (request == null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		Subgroup subgroup = subgroupRepository.findByIdAndGroup_IdAndStatusAndDeletedAtIsNull(
			subgroupId,
			groupId,
			SubgroupStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND));

		String updatedName = JsonNodePatchUtils.applyStringIfPresent(
			request.getName(), subgroup::updateName, false, true);
		JsonNodePatchUtils.applyStringIfPresent(request.getDescription(), subgroup::updateDescription, true, false);
		applyProfileImageFileUuid(request.getProfileImageFileUuid(), subgroup);

		if (updatedName != null
			&& subgroupRepository.existsByGroup_IdAndNameAndDeletedAtIsNullAndIdNot(groupId, updatedName, subgroupId)) {
			throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
		}
	}

	private void applyProfileImageFileUuid(JsonNode node, Subgroup subgroup) {
		if (node == null) {
			return;
		}
		if (node.isNull()) {
			fileService.clearDomainImages(DomainType.SUBGROUP, subgroup.getId());
			return;
		}
		if (!node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		applySubgroupImage(subgroup, node.asText());
	}

	private void validateAuthenticated(Long memberId) {
		if (memberId == null) {
			throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
		}
	}

	private Group getGroup(Long groupId) {
		return groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
	}

	private void validateGroupMember(Long groupId, Long memberId) {
		groupMemberRepository.findByGroupIdAndMember_IdAndDeletedAtIsNull(groupId, memberId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.NO_PERMISSION));
	}

	private void validateJoinType(SubgroupJoinType joinType, String password) {
		if (joinType == SubgroupJoinType.PASSWORD) {
			validatePasswordProvided(password);
			return;
		}
		if (password != null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private void validateJoinRequest(Subgroup subgroup, SubgroupJoinRequest request) {
		String password = request == null ? null : request.getPassword();
		if (subgroup.getJoinType() == SubgroupJoinType.PASSWORD) {
			validatePasswordProvided(password);
			if (subgroup.getJoinPassword() == null) {
				throw new BusinessException(CommonErrorCode.INVALID_DOMAIN_STATE);
			}
			if (!passwordEncoder.matches(password, subgroup.getJoinPassword())) {
				throw new BusinessException(SubgroupErrorCode.PASSWORD_MISMATCH);
			}
			return;
		}
		if (password != null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private void validatePasswordProvided(String password) {
		if (password == null || password.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private void applySubgroupImage(Subgroup subgroup, String fileUuid) {
		try {
			fileService.replaceDomainImage(DomainType.SUBGROUP, subgroup.getId(), fileUuid);
		} catch (DataIntegrityViolationException ex) {
			log.warn(
				"도메인 이미지 링크 저장 실패. domainType={}, domainId={}",
				DomainType.SUBGROUP,
				subgroup.getId(),
				ex);
		}
	}
}
