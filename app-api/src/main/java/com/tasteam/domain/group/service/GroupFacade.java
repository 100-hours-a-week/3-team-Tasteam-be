package com.tasteam.domain.group.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListItem;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.utils.JsonNodePatchUtils;
import com.tasteam.global.utils.PaginationParamUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupFacade {

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final MemberRepository memberRepository;
	private final SubgroupMemberRepository subgroupMemberRepository;
	private final SubgroupRepository subgroupRepository;
	private final GroupCreateService groupCreateService;
	private final GroupAuthService groupAuthService;
	private final GroupImageService groupImageService;

	@Transactional
	public GroupCreateResponse createGroup(GroupCreateRequest request) {
		if (groupRepository.existsByNameAndDeletedAtIsNull(request.name())) {
			throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
		}

		Group group = groupCreateService.create(request);
		groupImageService.attachLogoIfPresent(group.getId(), request.logoImageFileUuid());
		groupAuthService.saveInitialPasswordCode(group, request.code());
		return GroupCreateResponse.from(group);
	}

	@Transactional(readOnly = true)
	public GroupGetResponse getGroup(Long groupId) {
		Group group = getActiveGroup(groupId);
		String logoImageUrl = groupImageService.getPrimaryLogoImageUrl(group.getId());
		return new GroupGetResponse(
			new GroupGetResponse.GroupData(
				group.getId(),
				group.getName(),
				logoImageUrl,
				group.getAddress(),
				group.getDetailAddress(),
				group.getEmailDomain(),
				groupMemberRepository.countByGroupIdAndDeletedAtIsNull(group.getId()),
				group.getStatus().name(),
				group.getCreatedAt(),
				group.getUpdatedAt()));
	}

	@Transactional
	public void updateGroup(Long groupId, GroupUpdateRequest request) {
		Group group = getActiveGroup(groupId);
		JsonNodePatchUtils.applyStringIfPresent(request.name(), group::updateName, false, false);
		JsonNodePatchUtils.applyStringIfPresent(request.address(), group::updateAddress, false, false);
		JsonNodePatchUtils.applyStringIfPresent(request.detailAddress(), group::updateDetailAddress, true, false);
		JsonNodePatchUtils.applyStringIfPresent(request.emailDomain(), group::updateEmailDomain, true, false);
		groupImageService.applyLogoImagePatch(request.logoImageFileUuid(), group.getId());
		applyStatusIfPresent(request.status(), group);
	}

	@Transactional
	public GroupEmailVerificationResponse sendGroupEmailVerification(Long groupId, String email) {
		return groupAuthService.sendGroupEmailVerification(getActiveGroup(groupId), email);
	}

	@Transactional
	public GroupEmailAuthenticationResponse authenticateGroupByEmail(Long groupId, Long memberId, String code) {
		return groupAuthService.authenticateGroupByEmail(getActiveGroup(groupId), memberId, code);
	}

	@Transactional
	public GroupPasswordAuthenticationResponse authenticateGroupByPassword(Long groupId, Long memberId, String code) {
		return groupAuthService.authenticateGroupByPassword(getActiveGroup(groupId), memberId, code);
	}

	@Transactional
	public void deleteGroup(Long groupId) {
		Group group = getActiveGroup(groupId);
		group.delete(Instant.now());
	}

	@Transactional
	public void withdrawGroup(Long groupId, Long memberId) {
		Group group = getActiveGroup(groupId);
		GroupMember groupMember = groupMemberRepository.findByGroupIdAndMember_Id(
			group.getId(),
			memberId).orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
		if (groupMember.getDeletedAt() != null) {
			return;
		}
		groupMember.softDelete(Instant.now());

		List<SubgroupMember> subgroupMembers = subgroupMemberRepository.findActiveMembersByMemberAndGroup(
			groupId,
			memberId);
		if (subgroupMembers.isEmpty()) {
			return;
		}

		List<Long> subgroupIds = subgroupMembers.stream()
			.map(SubgroupMember::getSubgroupId)
			.toList();

		Map<Long, Subgroup> subgroupMap = subgroupRepository.findAllById(subgroupIds).stream()
			.collect(Collectors.toMap(Subgroup::getId, Function.identity()));

		Instant now = Instant.now();
		for (SubgroupMember subgroupMember : subgroupMembers) {
			subgroupMember.softDelete(now);
			Subgroup subgroup = subgroupMap.get(subgroupMember.getSubgroupId());
			if (subgroup != null) {
				subgroup.decreaseMemberCount();
			}
		}
	}

	@Transactional(readOnly = true)
	public GroupMemberListResponse getGroupMembers(Long groupId, String cursor, Integer size) {
		getActiveGroup(groupId);
		int resolvedSize = PaginationParamUtils.resolveSize(size);
		Long cursorId = PaginationParamUtils.parseLongCursor(cursor);

		List<GroupMemberListItem> items = groupMemberRepository.findGroupMembers(
			groupId,
			cursorId,
			PageRequest.of(0, resolvedSize + 1));

		boolean hasNext = items.size() > resolvedSize;
		if (hasNext) {
			items = items.subList(0, resolvedSize);
		}

		String nextCursor = hasNext ? String.valueOf(items.get(items.size() - 1).cursorId()) : null;
		return new GroupMemberListResponse(
			items,
			new GroupMemberListResponse.PageInfo(nextCursor, resolvedSize, hasNext));
	}

	@Transactional
	public void deleteGroupMember(Long groupId, Long userId) {
		getActiveGroup(groupId);
		memberRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		GroupMember groupMember = groupMemberRepository.findByGroupIdAndMember_IdAndDeletedAtIsNull(
			groupId,
			userId).orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
		groupMember.softDelete(Instant.now());
	}

	private Group getActiveGroup(Long groupId) {
		return groupRepository.findByIdAndDeletedAtIsNull(groupId)
			.orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_NOT_FOUND));
	}

	private void applyStatusIfPresent(JsonNode node, Group group) {
		if (node == null) {
			return;
		}
		if (node.isNull() || !node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		try {
			GroupStatus status = GroupStatus.valueOf(node.asText());
			group.updateStatus(status);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
