package com.tasteam.domain.subgroup.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.dto.SubgroupMemberCountCursor;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.dto.SubgroupNameCursor;
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
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;
import com.tasteam.global.utils.JsonNodePatchUtils;
import com.tasteam.global.utils.PaginationParamUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubgroupService {

	private static final String SORT_NAME_ASC_ID_ASC = "NAME_ASC_ID_ASC";

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final MemberRepository memberRepository;
	private final SubgroupRepository subgroupRepository;
	private final SubgroupMemberRepository subgroupMemberRepository;
	private final PasswordEncoder passwordEncoder;
	private final CursorCodec cursorCodec;
	private final FileService fileService;

	@Transactional(readOnly = true)
	public SubgroupListResponse getMySubgroups(Long groupId, Long memberId, String keyword, String cursor,
		Integer size) {
		validateAuthenticated(memberId);
		Group group = getGroup(groupId);
		validateGroupMember(group.getId(), memberId);

		int resolvedSize = PaginationParamUtils.resolveSize(size);
		CursorPageBuilder<SubgroupNameCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursor,
			SubgroupNameCursor.class);
		if (pageBuilder.isInvalid()) {
			return emptyListResponse(resolvedSize);
		}

		SubgroupNameCursor cursorKey = pageBuilder.cursor();
		String resolvedKeyword = resolveKeyword(keyword);

		List<SubgroupListItem> items = subgroupRepository.findMySubgroupsByGroup(
			groupId,
			memberId,
			resolvedKeyword,
			cursorKey == null ? null : cursorKey.name(),
			cursorKey == null ? null : cursorKey.id(),
			PageRequest.of(0, resolvedSize + 1));

		CursorPageBuilder.Page<SubgroupListItem> page = pageBuilder.build(
			applyResolvedImageUrls(items),
			resolvedSize,
			last -> new SubgroupNameCursor(last.getName(), last.getSubgroupId()));

		return buildListResponse(page, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupListItem> getGroupSubgroups(Long groupId, Long memberId, String cursor,
		Integer size) {
		getGroup(groupId);

		int resolvedSize = PaginationParamUtils.resolveSize(size);
		CursorPageBuilder<SubgroupNameCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursor,
			SubgroupNameCursor.class);
		if (pageBuilder.isInvalid()) {
			return CursorPageResponse.empty();
		}
		SubgroupNameCursor cursorKey = pageBuilder.cursor();

		List<SubgroupListItem> items = subgroupRepository.findSubgroupsByGroup(
			groupId,
			SubgroupStatus.ACTIVE,
			cursorKey == null ? null : cursorKey.name(),
			cursorKey == null ? null : cursorKey.id(),
			PageRequest.of(0, resolvedSize + 1));

		CursorPageBuilder.Page<SubgroupListItem> page = pageBuilder.build(
			applyResolvedImageUrls(items),
			resolvedSize,
			last -> new SubgroupNameCursor(last.getName(), last.getSubgroupId()));

		return buildCursorPageResponse(page);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupListItem> searchGroupSubgroups(Long groupId, String keyword, String cursor,
		Integer size) {
		getGroup(groupId);

		int resolvedSize = PaginationParamUtils.resolveSize(size);
		CursorPageBuilder<SubgroupMemberCountCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursor,
			SubgroupMemberCountCursor.class);
		if (pageBuilder.isInvalid()) {
			return CursorPageResponse.empty();
		}

		SubgroupMemberCountCursor cursorKey = pageBuilder.cursor();
		String resolvedKeyword = resolveKeyword(keyword);

		List<SubgroupListItem> items = subgroupRepository.searchSubgroupsByGroup(
			groupId,
			SubgroupStatus.ACTIVE,
			resolvedKeyword,
			cursorKey == null ? null : cursorKey.memberCount(),
			cursorKey == null ? null : cursorKey.id(),
			PageRequest.of(0, resolvedSize + 1));

		CursorPageBuilder.Page<SubgroupListItem> page = pageBuilder.build(
			applyResolvedImageUrls(items),
			resolvedSize,
			last -> new SubgroupMemberCountCursor(last.getMemberCount(), last.getSubgroupId()));

		return buildCursorPageResponse(page);
	}

	@Transactional(readOnly = true)
	public SubgroupDetailResponse getSubgroup(Long subgroupId, Long memberId) {
		validateAuthenticated(memberId);

		Subgroup subgroup = subgroupRepository.findByIdAndDeletedAtIsNull(subgroupId)
			.orElseThrow(() -> new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND));

		if (subgroup.getJoinType() == SubgroupJoinType.PASSWORD) {
			subgroupMemberRepository.findBySubgroupIdAndMember_IdAndDeletedAtIsNull(subgroupId, memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NO_PERMISSION));
		}

		String profileImageUrl = fileService.getPrimaryDomainImageUrl(DomainType.SUBGROUP, subgroup.getId());

		return new SubgroupDetailResponse(
			new SubgroupDetailResponse.SubgroupDetail(
				subgroup.getGroup().getId(),
				subgroup.getId(),
				subgroup.getName(),
				subgroup.getDescription(),
				subgroup.getMemberCount(),
				profileImageUrl,
				subgroup.getCreatedAt()));
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupMemberListItem> getSubgroupMembers(Long subgroupId, String cursor,
		Integer size) {
		if (!subgroupRepository.existsByIdAndStatus(subgroupId, SubgroupStatus.ACTIVE)) {
			throw new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND);
		}

		int resolvedSize = PaginationParamUtils.resolveSize(size);
		Long cursorId = PaginationParamUtils.parseLongCursor(cursor);

		List<SubgroupMemberListItem> items = subgroupMemberRepository.findSubgroupMembers(
			subgroupId,
			cursorId,
			PageRequest.of(0, resolvedSize + 1));

		boolean hasNext = items.size() > resolvedSize;
		if (hasNext) {
			items = items.subList(0, resolvedSize);
		}

		String nextCursor = hasNext ? String.valueOf(items.get(items.size() - 1).cursorId()) : null;

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(nextCursor, hasNext, items.size()));
	}

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

	private SubgroupListResponse buildListResponse(CursorPageBuilder.Page<SubgroupListItem> page, int resolvedSize) {
		return new SubgroupListResponse(
			page.items(),
			new SubgroupListResponse.PageInfo(
				SORT_NAME_ASC_ID_ASC,
				page.nextCursor(),
				page.requestedSize(),
				page.hasNext()));
	}

	private CursorPageResponse<SubgroupListItem> buildCursorPageResponse(
		CursorPageBuilder.Page<SubgroupListItem> page) {
		return new CursorPageResponse<>(
			page.items(),
			new CursorPageResponse.Pagination(
				page.nextCursor(),
				page.hasNext(),
				page.size()));
	}

	private SubgroupListResponse emptyListResponse(int resolvedSize) {
		return new SubgroupListResponse(
			List.of(),
			new SubgroupListResponse.PageInfo(
				SORT_NAME_ASC_ID_ASC,
				null,
				resolvedSize,
				false));
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

	private String resolveKeyword(String keyword) {
		if (keyword == null) {
			return null;
		}
		String trimmed = keyword.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private void applySubgroupImage(Subgroup subgroup, String fileUuid) {
		try {
			fileService.replaceDomainImage(DomainType.SUBGROUP, subgroup.getId(), fileUuid);
		} catch (DataIntegrityViolationException ex) {
			// DB enum/check가 코드보다 늦게 반영된 환경에서도 생성/수정을 막지 않기 위한 fallback.
			log.warn(
				"도메인 이미지 링크 저장 실패. domainType={}, domainId={}",
				DomainType.SUBGROUP,
				subgroup.getId(),
				ex);
		}
	}

	private List<SubgroupListItem> applyResolvedImageUrls(List<SubgroupListItem> items) {
		if (items.isEmpty()) {
			return items;
		}
		Map<Long, String> imageUrlBySubgroupId = resolveImageUrlByDomainId(
			items.stream().map(SubgroupListItem::getSubgroupId).toList());

		return items.stream()
			.map(item -> SubgroupListItem.builder()
				.subgroupId(item.getSubgroupId())
				.name(item.getName())
				.description(item.getDescription())
				.memberCount(item.getMemberCount())
				.profileImageUrl(imageUrlBySubgroupId.get(item.getSubgroupId()))
				.joinType(item.getJoinType())
				.createdAt(item.getCreatedAt())
				.build())
			.toList();
	}

	private Map<Long, String> resolveImageUrlByDomainId(List<Long> domainIds) {
		if (domainIds.isEmpty()) {
			return Map.of();
		}
		return fileService.getPrimaryDomainImageUrlMap(DomainType.SUBGROUP, domainIds);
	}
}
