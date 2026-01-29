package com.tasteam.domain.subgroup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.subgroup.controller.docs.SubgroupManagementControllerDocs;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.domain.subgroup.service.SubgroupService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubgroupManagementController implements SubgroupManagementControllerDocs {

	private final SubgroupService subgroupService;

	@GetMapping("/members/me/groups/{groupId}/subgroups")
	public SuccessResponse<SubgroupListResponse> getMySubgroups(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestParam(required = false)
		String keyword,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(subgroupService.getMySubgroups(groupId, memberId, keyword, cursor, size));
	}

	@GetMapping("/groups/{groupId}/subgroups")
	public SuccessResponse<SubgroupListResponse> getGroupSubgroups(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestParam(required = false)
		String keyword,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		Integer size) {
		return SuccessResponse.success(subgroupService.getGroupSubgroups(groupId, memberId, keyword, cursor, size));
	}

	@GetMapping("/subgroups/{subgroupId}")
	public SuccessResponse<SubgroupDetailResponse> getSubgroup(
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId) {
		return SuccessResponse.success(subgroupService.getSubgroup(subgroupId, memberId));
	}

	@PostMapping("/groups/{groupId}/subgroups")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<SubgroupCreateResponse> createSubgroup(
		@PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		SubgroupCreateRequest request) {
		return SuccessResponse.success(subgroupService.createSubgroup(groupId, memberId, request));
	}

	@PostMapping("/groups/{groupId}/subgroups/{subgroupId}/members")
	public SuccessResponse<SubgroupJoinResponse> joinSubgroup(
		@PathVariable @Positive
		Long groupId,
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId,
		@RequestBody(required = false)
		SubgroupJoinRequest request) {
		return SuccessResponse.success(subgroupService.joinSubgroup(groupId, subgroupId, memberId, request));
	}

	@DeleteMapping("/subgroups/{subgroupId}/members/me")
	public ResponseEntity<Void> withdrawSubgroup(
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId) {
		subgroupService.withdrawSubgroup(subgroupId, memberId);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/groups/{groupId}/subgroups/{subgroupId}")
	public SuccessResponse<Void> updateSubgroup(
		@PathVariable @Positive
		Long groupId,
		@PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		SubgroupUpdateRequest request) {
		subgroupService.updateSubgroup(groupId, subgroupId, memberId, request);
		return SuccessResponse.success(null);
	}
}
