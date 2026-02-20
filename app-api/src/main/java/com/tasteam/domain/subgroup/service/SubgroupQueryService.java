package com.tasteam.domain.subgroup.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.dto.SubgroupMemberCountCursor;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.dto.SubgroupNameCursor;
import com.tasteam.domain.subgroup.entity.Subgroup;
import com.tasteam.domain.subgroup.repository.SubgroupMemberRepository;
import com.tasteam.domain.subgroup.repository.SubgroupRepository;
import com.tasteam.domain.subgroup.type.SubgroupJoinType;
import com.tasteam.domain.subgroup.type.SubgroupStatus;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.GroupErrorCode;
import com.tasteam.global.exception.code.SearchErrorCode;
import com.tasteam.global.exception.code.SubgroupErrorCode;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;
import com.tasteam.global.utils.PaginationParamUtils;
import com.tasteam.global.validation.KeywordSecurityPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubgroupQueryService {

	private static final String SORT_NAME_ASC_ID_ASC = "NAME_ASC_ID_ASC";

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final SubgroupRepository subgroupRepository;
	private final SubgroupMemberRepository subgroupMemberRepository;
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

	private String resolveKeyword(String keyword) {
		if (keyword == null) {
			return null;
		}
		String trimmed = keyword.trim();
		if (trimmed.isBlank()) {
			return null;
		}
		if (!KeywordSecurityPolicy.isSafeKeyword(trimmed)) {
			throw new BusinessException(SearchErrorCode.INVALID_SEARCH_KEYWORD);
		}
		return trimmed;
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
