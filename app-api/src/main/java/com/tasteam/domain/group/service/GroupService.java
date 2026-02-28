package com.tasteam.domain.group.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

	private final GroupFacade groupFacade;

	@Transactional
	public GroupCreateResponse createGroup(GroupCreateRequest request) {
		return groupFacade.createGroup(request);
	}

	@Transactional(readOnly = true)
	public GroupGetResponse getGroup(Long groupId) {
		return groupFacade.getGroup(groupId);
	}

	@Transactional
	public void updateGroup(Long groupId, GroupUpdateRequest request) {
		groupFacade.updateGroup(groupId, request);
	}

	@Transactional
	public GroupEmailVerificationResponse sendGroupEmailVerification(Long groupId, Long memberId, String clientIp,
		String email) {
		return groupFacade.sendGroupEmailVerification(groupId, memberId, clientIp, email);
	}

	@Transactional
	public GroupEmailAuthenticationResponse authenticateGroupByEmail(Long groupId, Long memberId, String token) {
		return groupFacade.authenticateGroupByEmail(groupId, memberId, token);
	}

	@Transactional
	public GroupPasswordAuthenticationResponse authenticateGroupByPassword(Long groupId, Long memberId, String code) {
		return groupFacade.authenticateGroupByPassword(groupId, memberId, code);
	}

	@Transactional
	public void deleteGroup(Long groupId) {
		groupFacade.deleteGroup(groupId);
	}

	@Transactional
	public void withdrawGroup(Long groupId, Long memberId) {
		groupFacade.withdrawGroup(groupId, memberId);
	}

	@Transactional(readOnly = true)
	public GroupMemberListResponse getGroupMembers(Long groupId, String cursor, Integer size) {
		return groupFacade.getGroupMembers(groupId, cursor, size);
	}

	@Transactional
	public void deleteGroupMember(Long groupId, Long userId) {
		groupFacade.deleteGroupMember(groupId, userId);
	}
}
