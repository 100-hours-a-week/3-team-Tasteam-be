package com.tasteam.domain.subgroup.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.subgroup.dto.SubgroupChatRoomResponse;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubgroupFacade {

	private final SubgroupQueryService subgroupQueryService;
	private final SubgroupCommandService subgroupCommandService;

	@Transactional(readOnly = true)
	public SubgroupListResponse getMySubgroups(Long groupId, Long memberId, String keyword, String cursor,
		Integer size) {
		return subgroupQueryService.getMySubgroups(groupId, memberId, keyword, cursor, size);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupListItem> getGroupSubgroups(Long groupId, Long memberId, String cursor,
		Integer size) {
		return subgroupQueryService.getGroupSubgroups(groupId, memberId, cursor, size);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupListItem> searchGroupSubgroups(Long groupId, String keyword, String cursor,
		Integer size) {
		return subgroupQueryService.searchGroupSubgroups(groupId, keyword, cursor, size);
	}

	@Transactional(readOnly = true)
	public SubgroupDetailResponse getSubgroup(Long subgroupId, Long memberId) {
		return subgroupQueryService.getSubgroup(subgroupId, memberId);
	}

	@Transactional(readOnly = true)
	public SubgroupChatRoomResponse getChatRoom(Long subgroupId, Long memberId) {
		return subgroupQueryService.getChatRoom(subgroupId, memberId);
	}

	@Transactional(readOnly = true)
	public CursorPageResponse<SubgroupMemberListItem> getSubgroupMembers(Long subgroupId, Long memberId, String cursor,
		Integer size) {
		return subgroupQueryService.getSubgroupMembers(subgroupId, memberId, cursor, size);
	}

	@Transactional
	public SubgroupCreateResponse createSubgroup(Long groupId, Long memberId, SubgroupCreateRequest request) {
		return subgroupCommandService.createSubgroup(groupId, memberId, request);
	}

	@Transactional
	public SubgroupJoinResponse joinSubgroup(Long groupId, Long subgroupId, Long memberId,
		SubgroupJoinRequest request) {
		return subgroupCommandService.joinSubgroup(groupId, subgroupId, memberId, request);
	}

	@Transactional
	public void withdrawSubgroup(Long subgroupId, Long memberId) {
		subgroupCommandService.withdrawSubgroup(subgroupId, memberId);
	}

	@Transactional
	public void updateSubgroup(Long groupId, Long subgroupId, Long memberId, SubgroupUpdateRequest request) {
		subgroupCommandService.updateSubgroup(groupId, subgroupId, memberId, request);
	}
}
