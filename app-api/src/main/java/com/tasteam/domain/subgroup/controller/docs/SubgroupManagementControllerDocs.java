package com.tasteam.domain.subgroup.controller.docs;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListResponse;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Subgroup", description = "소모임 관리 API")
public interface SubgroupManagementControllerDocs {

	@Operation(summary = "내 소모임 목록 조회", description = "내가 속한 소모임 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = SubgroupListResponse.class)))
	SuccessResponse<SubgroupListResponse> getMySubgroups(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable
		Long groupId,
		@CurrentUser
		Long memberId,
		@Parameter(description = "검색 키워드", example = "맛집") @RequestParam(required = false)
		String keyword,
		@Parameter(description = "페이징 커서", example = "cursor") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false)
		Integer size);

	@Operation(summary = "그룹 소모임 목록 조회", description = "그룹에 속한 소모임 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = SubgroupListResponse.class)))
	SuccessResponse<SubgroupListResponse> getGroupSubgroups(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable
		Long groupId,
		@CurrentUser
		Long memberId,
		@Parameter(description = "검색 키워드", example = "맛집") @RequestParam(required = false)
		String keyword,
		@Parameter(description = "페이징 커서", example = "cursor") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false)
		Integer size);

	@Operation(summary = "소모임 상세 조회", description = "소모임 ID로 상세 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = SubgroupDetailResponse.class)))
	SuccessResponse<SubgroupDetailResponse> getSubgroup(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable
		Long subgroupId,
		@CurrentUser
		Long memberId);

	@Operation(summary = "소모임 생성", description = "그룹 내 소모임을 생성합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = SubgroupCreateRequest.class)))
	@ApiResponse(responseCode = "201", description = "생성 완료", content = @Content(schema = @Schema(implementation = SubgroupCreateResponse.class)))
	SuccessResponse<SubgroupCreateResponse> createSubgroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable
		Long groupId,
		@CurrentUser
		Long memberId,
		SubgroupCreateRequest request);

	@Operation(summary = "소모임 가입", description = "소모임에 가입합니다. 비공개 소모임인 경우 비밀번호를 포함합니다.")
	@RequestBody(required = false, content = @Content(schema = @Schema(implementation = SubgroupJoinRequest.class)))
	@ApiResponse(responseCode = "200", description = "가입 완료", content = @Content(schema = @Schema(implementation = SubgroupJoinResponse.class)))
	SuccessResponse<SubgroupJoinResponse> joinSubgroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable
		Long groupId,
		@Parameter(description = "소모임 ID", example = "301") @PathVariable
		Long subgroupId,
		@CurrentUser
		Long memberId,
		SubgroupJoinRequest request);

	@Operation(summary = "소모임 탈퇴", description = "현재 로그인 사용자가 소모임에서 탈퇴합니다.")
	@ApiResponse(responseCode = "204", description = "탈퇴 완료")
	org.springframework.http.ResponseEntity<Void> withdrawSubgroup(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable
		Long subgroupId,
		@CurrentUser
		Long memberId);

	@Operation(summary = "소모임 수정", description = "소모임 이름/설명을 수정합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = SubgroupUpdateRequest.class)))
	@ApiResponse(responseCode = "200", description = "수정 완료")
	SuccessResponse<Void> updateSubgroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable
		Long groupId,
		@Parameter(description = "소모임 ID", example = "301") @PathVariable
		Long subgroupId,
		@CurrentUser
		Long memberId,
		SubgroupUpdateRequest request);
}
