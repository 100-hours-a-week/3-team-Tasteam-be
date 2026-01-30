package com.tasteam.domain.subgroup.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.tasteam.domain.file.dto.response.ImageUrlResponse;
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

import lombok.RequiredArgsConstructor;

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

		int resolvedSize = resolveSize(size);
		SubgroupNameCursor cursorKey = cursorCodec.decodeOrNull(cursor, SubgroupNameCursor.class);
		String resolvedKeyword = resolveKeyword(keyword);

		List<SubgroupListItem> items = subgroupRepository.findMySubgroupsByGroup(
			groupId,
			memberId,
			resolvedKeyword,
			cursorKey == null ? null : cursorKey.name(),
			cursorKey == null ? null : cursorKey.id(),
			PageRequest.of(0, resolvedSize + 1));

		return buildListResponse(items, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupListItem> getGroupSubgroups(Long groupId, Long memberId, String cursor,
		Integer size) {
		validateAuthenticated(memberId);
		getGroup(groupId);

		int resolvedSize = resolveSize(size);
		SubgroupNameCursor cursorKey = cursorCodec.decodeOrNull(cursor, SubgroupNameCursor.class);

		List<SubgroupListItem> items = subgroupRepository.findSubgroupsByGroup(
			groupId,
			SubgroupStatus.ACTIVE,
			cursorKey == null ? null : cursorKey.name(),
			cursorKey == null ? null : cursorKey.id(),
			PageRequest.of(0, resolvedSize + 1));

		return buildNameCursorPageResponse(items, resolvedSize);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupListItem> searchGroupSubgroups(Long groupId, String keyword, String cursor,
		Integer size) {
		getGroup(groupId);

		int resolvedSize = resolveSize(size);
		SubgroupMemberCountCursor cursorKey = cursorCodec.decodeOrNull(cursor, SubgroupMemberCountCursor.class);
		String resolvedKeyword = resolveKeyword(keyword);

		List<SubgroupListItem> items = subgroupRepository.searchSubgroupsByGroup(
			groupId,
			SubgroupStatus.ACTIVE,
			resolvedKeyword,
			cursorKey == null ? null : cursorKey.memberCount(),
			cursorKey == null ? null : cursorKey.id(),
			PageRequest.of(0, resolvedSize + 1));

		return buildMemberCountCursorPageResponse(items, resolvedSize);
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

		return new SubgroupDetailResponse(
			new SubgroupDetailResponse.SubgroupDetail(
				subgroup.getGroup().getId(),
				subgroup.getId(),
				subgroup.getName(),
				subgroup.getDescription(),
				subgroup.getMemberCount(),
				buildProfileImage(subgroup),
				subgroup.getCreatedAt()));
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupMemberListItem> getSubgroupMembers(Long subgroupId, String cursor,
		Integer size) {
		if (!subgroupRepository.existsByIdAndStatus(subgroupId, SubgroupStatus.ACTIVE)) {
			throw new BusinessException(SubgroupErrorCode.SUBGROUP_NOT_FOUND);
		}

		int resolvedSize = resolveSize(size);
		Long cursorId = parseCursor(cursor);

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

		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		subgroupMemberRepository.save(SubgroupMember.create(subgroup.getId(), member));
		applyProfileImageToSubgroup(subgroup, request.getProfileImageId());

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

		String updatedName = applyStringIfPresent(request.getName(), subgroup::updateName, false);
		applyStringIfPresent(request.getDescription(), subgroup::updateDescription, true);
		applyProfileImageId(request.getProfileImageId(), subgroup);

		if (updatedName != null
			&& subgroupRepository.existsByGroup_IdAndNameAndDeletedAtIsNullAndIdNot(groupId, updatedName, subgroupId)) {
			throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
		}
	}

	private SubgroupListResponse buildListResponse(List<SubgroupListItem> items, int resolvedSize) {
		boolean hasNext = items.size() > resolvedSize;
		if (hasNext) {
			items = items.subList(0, resolvedSize);
		}

		String nextCursor = null;
		if (hasNext && !items.isEmpty()) {
			SubgroupListItem lastItem = items.get(items.size() - 1);
			nextCursor = cursorCodec.encode(new SubgroupNameCursor(lastItem.getName(), lastItem.getSubgroupId()));
		}

		return new SubgroupListResponse(
			items,
			new SubgroupListResponse.PageInfo(
				SORT_NAME_ASC_ID_ASC,
				nextCursor,
				resolvedSize,
				hasNext));
	}

	private CursorPageResponse<SubgroupListItem> buildNameCursorPageResponse(List<SubgroupListItem> items,
		int resolvedSize) {
		boolean hasNext = items.size() > resolvedSize;
		if (hasNext) {
			items = items.subList(0, resolvedSize);
		}

		String nextCursor = null;
		if (hasNext && !items.isEmpty()) {
			SubgroupListItem lastItem = items.get(items.size() - 1);
			nextCursor = cursorCodec.encode(new SubgroupNameCursor(lastItem.getName(), lastItem.getSubgroupId()));
		}

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				nextCursor,
				hasNext,
				items.size()));
	}

	private CursorPageResponse<SubgroupListItem> buildMemberCountCursorPageResponse(List<SubgroupListItem> items,
		int resolvedSize) {
		boolean hasNext = items.size() > resolvedSize;
		if (hasNext) {
			items = items.subList(0, resolvedSize);
		}

		String nextCursor = null;
		if (hasNext && !items.isEmpty()) {
			SubgroupListItem lastItem = items.get(items.size() - 1);
			nextCursor = cursorCodec
				.encode(new SubgroupMemberCountCursor(lastItem.getMemberCount(), lastItem.getSubgroupId()));
		}

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				nextCursor,
				hasNext,
				items.size()));
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

	private String applyStringIfPresent(JsonNode node, Consumer<String> updater, boolean nullable) {
		if (node == null) {
			return null;
		}
		if (node.isNull()) {
			if (!nullable) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
			}
			updater.accept(null);
			return null;
		}
		if (!node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		String value = node.asText();
		if (!nullable && value.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		updater.accept(value);
		return value;
	}

	private void applyProfileImageId(JsonNode node, Subgroup subgroup) {
		if (node == null) {
			return;
		}
		if (node.isNull()) {
			fileService.unlinkDomainImages(DomainType.SUBGROUP, subgroup.getId());
			subgroup.clearProfileImage();
			return;
		}
		if (!node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		linkSubgroupProfileImage(subgroup, node.asText());
	}

	private void applyProfileImageToSubgroup(Subgroup subgroup, String profileImageId) {
		if (profileImageId == null) {
			return;
		}
		linkSubgroupProfileImage(subgroup, profileImageId);
	}

	private void linkSubgroupProfileImage(Subgroup subgroup, String profileImageId) {
		if (profileImageId == null || profileImageId.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		ImageUrlResponse imageUrl = fileService.linkDomainImageAndGetUrl(
			DomainType.SUBGROUP,
			subgroup.getId(),
			profileImageId);
		subgroup.updateProfileImage(imageUrl.url(), UUID.fromString(imageUrl.fileUuid()));
	}

	private SubgroupDetailResponse.ProfileImage buildProfileImage(Subgroup subgroup) {
		if (subgroup.getProfileImageUuid() == null || subgroup.getProfileImageUrl() == null) {
			return null;
		}
		return new SubgroupDetailResponse.ProfileImage(
			subgroup.getProfileImageUuid(),
			subgroup.getProfileImageUrl());
	}

	private int resolveSize(Integer size) {
		if (size == null) {
			return 10;
		}
		if (size < 1 || size > 100) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return size;
	}

	private String resolveKeyword(String keyword) {
		if (keyword == null) {
			return null;
		}
		String trimmed = keyword.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private Long parseCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(cursor);
		} catch (NumberFormatException e) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
