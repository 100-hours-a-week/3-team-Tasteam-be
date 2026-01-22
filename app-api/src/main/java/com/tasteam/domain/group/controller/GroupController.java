package com.tasteam.domain.group.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationRequest;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationRequest;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.group.service.GroupService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Validated
public class GroupController {

	private final GroupService groupService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupCreateResponse> createGroup(@Valid @RequestBody
	GroupCreateRequest request) {
		return SuccessResponse.success(groupService.createGroup(request));
	}

	@GetMapping("/{groupId}")
	public SuccessResponse<GroupGetResponse> getGroup(@PathVariable @Positive
	Long groupId) {
		return SuccessResponse.success(groupService.getGroup(groupId));
	}

	@PatchMapping("/{groupId}")
	public SuccessResponse<Void> updateGroup(
		@PathVariable @Positive
		Long groupId,
		@RequestBody
		GroupUpdateRequest request) {
		groupService.updateGroup(groupId, request);
		return SuccessResponse.success(null);
	}

	@DeleteMapping("/{groupId}")
	public SuccessResponse<Void> deleteGroup(@PathVariable @Positive
	Long groupId) {
		groupService.deleteGroup(groupId);
		return SuccessResponse.success(null);
	}

	@DeleteMapping("/{groupId}/members/me")
	public SuccessResponse<Void> withdrawGroup(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId) {
		groupService.withdrawGroup(groupId, memberId);
		return SuccessResponse.success(null);
	}

	@PostMapping("/{groupId}/email-verifications")
	public SuccessResponse<GroupEmailVerificationResponse> sendGroupEmailVerification(
		@PathVariable @Positive
		Long groupId,
		@Valid @RequestBody
		GroupEmailVerificationRequest request) {
		return SuccessResponse.success(groupService.sendGroupEmailVerification(groupId, request.getEmail()));
	}

	@PostMapping("/{groupId}/email-authentications")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupEmailAuthenticationResponse> authenticateGroupByEmail(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@Valid @RequestBody
		GroupEmailAuthenticationRequest request) {
		return SuccessResponse.success(
			groupService.authenticateGroupByEmail(groupId, memberId, request.getCode()));
	}

	@GetMapping("/{groupId}/members")
	public SuccessResponse<GroupMemberListResponse> getGroupMembers(
		@PathVariable @Positive
		Long groupId,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(groupService.getGroupMembers(groupId, cursor, size));
	}

	@DeleteMapping("/{groupId}/members/{userId}")
	public SuccessResponse<Void> deleteGroupMember(
		@PathVariable @Positive
		Long groupId,
		@PathVariable @Positive
		Long userId) {
		groupService.deleteGroupMember(groupId, userId);
		return SuccessResponse.success(null);
	}
}
