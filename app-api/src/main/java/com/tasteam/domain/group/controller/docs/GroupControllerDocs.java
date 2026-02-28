package com.tasteam.domain.group.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationRequest;
import com.tasteam.domain.group.dto.GroupEmailAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationRequest;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.dto.GroupMemberListResponse;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationRequest;
import com.tasteam.domain.group.dto.GroupPasswordAuthenticationResponse;
import com.tasteam.domain.group.dto.GroupUpdateRequest;
import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.subgroup.dto.SubgroupCreateRequest;
import com.tasteam.domain.subgroup.dto.SubgroupCreateResponse;
import com.tasteam.domain.subgroup.dto.SubgroupJoinRequest;
import com.tasteam.domain.subgroup.dto.SubgroupJoinResponse;
import com.tasteam.domain.subgroup.dto.SubgroupListItem;
import com.tasteam.domain.subgroup.dto.SubgroupUpdateRequest;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;
import com.tasteam.global.swagger.error.code.group.GroupSwaggerErrorResponseDescription;
import com.tasteam.global.swagger.error.code.subgroup.SubgroupSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;

@SwaggerTagOrder(15)
@Tag(name = "Group", description = "그룹 관리 API")
public interface GroupControllerDocs {

	@Operation(summary = "그룹 생성", description = "새 그룹을 생성합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = GroupCreateRequest.class)))
	@ApiResponse(responseCode = "201", description = "그룹 생성 완료", content = @Content(schema = @Schema(implementation = GroupCreateResponse.class)))
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_CREATE")
	SuccessResponse<GroupCreateResponse> createGroup(@Validated
	GroupCreateRequest request);

	@Operation(summary = "그룹 상세 조회", description = "그룹 ID로 그룹 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = GroupGetResponse.class)))
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_GET")
	SuccessResponse<GroupGetResponse> getGroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId);

	@Operation(summary = "그룹 정보 수정", description = "그룹 이름/설명을 수정합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = GroupUpdateRequest.class)))
	@ApiResponse(responseCode = "200", description = "수정 완료")
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_UPDATE")
	SuccessResponse<Void> updateGroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@Validated
		GroupUpdateRequest request);

	@Operation(summary = "그룹 삭제", description = "그룹을 삭제합니다.")
	@ApiResponse(responseCode = "200", description = "삭제 완료")
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_DELETE")
	SuccessResponse<Void> deleteGroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId);

	@Operation(summary = "그룹 멤버 목록 조회", description = "그룹 멤버를 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = GroupMemberListResponse.class)))
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_MEMBERS")
	SuccessResponse<GroupMemberListResponse> getGroupMembers(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@Parameter(description = "페이징 커서", example = "cursor") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false)
		Integer size);

	@Operation(summary = "그룹 소모임 목록 조회", description = "그룹에 속한 소모임 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_LIST_GROUP")
	SuccessResponse<CursorPageResponse<SubgroupListItem>> getGroupSubgroups(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@Parameter(description = "페이징 커서", example = "cursor") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false)
		Integer size);

	@Operation(summary = "그룹 리뷰 목록 조회", description = "그룹에 속한 리뷰를 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_REVIEWS")
	SuccessResponse<CursorPageResponse<ReviewResponse>> getGroupReviews(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@ParameterObject
		RestaurantReviewListRequest request);

	@Operation(summary = "그룹 소모임 검색", description = "그룹에 속한 소모임을 검색합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_LIST_GROUP")
	SuccessResponse<CursorPageResponse<SubgroupListItem>> searchSubgroups(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@Parameter(description = "검색 키워드", example = "맛집") @RequestParam(required = false)
		String keyword,
		@Parameter(description = "페이징 커서", example = "cursor") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false)
		Integer size);

	@Operation(summary = "소모임 생성", description = "그룹 내 소모임을 생성합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = SubgroupCreateRequest.class)))
	@ApiResponse(responseCode = "201", description = "생성 완료", content = @Content(schema = @Schema(implementation = SubgroupCreateResponse.class)))
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_CREATE")
	SuccessResponse<SubgroupCreateResponse> createSubgroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@Validated
		SubgroupCreateRequest request);

	@Operation(summary = "그룹 탈퇴", description = "현재 로그인 사용자가 그룹에서 탈퇴합니다.")
	@ApiResponse(responseCode = "200", description = "탈퇴 완료")
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_WITHDRAW")
	SuccessResponse<Void> withdrawGroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId);

	@Operation(summary = "그룹 멤버 강퇴", description = "그룹 멤버를 강퇴합니다.")
	@ApiResponse(responseCode = "200", description = "강퇴 완료")
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_MEMBER_DELETE")
	SuccessResponse<Void> deleteGroupMember(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@Parameter(description = "대상 사용자 ID", example = "2001") @PathVariable @Positive
		Long userId);

	@Operation(summary = "그룹 이메일 인증 링크 발송", description = "그룹 가입 이메일 인증 링크를 발송합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = GroupEmailVerificationRequest.class)))
	@ApiResponse(responseCode = "200", description = "이메일 인증 링크 발송 성공", content = @Content(schema = @Schema(implementation = GroupEmailVerificationResponse.class)))
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_EMAIL_VERIFICATION")
	SuccessResponse<GroupEmailVerificationResponse> sendGroupEmailVerification(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		HttpServletRequest servletRequest,
		@Validated
		GroupEmailVerificationRequest request);

	@Operation(summary = "그룹 이메일 인증", description = "이메일 인증 토큰을 검증하여 그룹에 가입합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = GroupEmailAuthenticationRequest.class)))
	@ApiResponse(responseCode = "200", description = "이메일 인증 성공", content = @Content(schema = @Schema(implementation = GroupEmailAuthenticationResponse.class)))
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_EMAIL_AUTHENTICATION")
	SuccessResponse<GroupEmailAuthenticationResponse> authenticateGroupByEmail(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@Validated
		GroupEmailAuthenticationRequest request);

	@Operation(summary = "그룹 이메일 인증 링크 처리", description = "이메일 인증 링크의 토큰을 검증하여 그룹에 가입합니다.")
	@ApiResponse(responseCode = "200", description = "이메일 인증 성공", content = @Content(schema = @Schema(implementation = GroupEmailAuthenticationResponse.class)))
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_EMAIL_AUTHENTICATION")
	SuccessResponse<GroupEmailAuthenticationResponse> authenticateGroupByEmailByLink(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@Parameter(description = "이메일 인증 토큰", required = true) @RequestParam
		String token);

	@Operation(summary = "그룹 비밀번호 인증", description = "그룹 비밀번호 코드를 검증하여 그룹에 가입합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = GroupPasswordAuthenticationRequest.class)))
	@ApiResponse(responseCode = "201", description = "비밀번호 인증 성공", content = @Content(schema = @Schema(implementation = GroupPasswordAuthenticationResponse.class)))
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_PASSWORD_AUTHENTICATION")
	SuccessResponse<GroupPasswordAuthenticationResponse> authenticateGroupByPassword(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@CurrentUser
		Long memberId,
		@Validated
		GroupPasswordAuthenticationRequest request);

	@Operation(summary = "그룹 리뷰 음식점 목록 조회", description = "그룹 리뷰가 존재하는 음식점 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@CustomErrorResponseDescription(value = GroupSwaggerErrorResponseDescription.class, group = "GROUP_REVIEW_RESTAURANTS")
	SuccessResponse<CursorPageResponse<RestaurantListItem>> getGroupReviewRestaurants(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@Validated @ParameterObject
		NearbyRestaurantQueryParams queryParams);

	@Operation(summary = "소모임 수정", description = "소모임 이름/설명을 수정합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = SubgroupUpdateRequest.class)))
	@ApiResponse(responseCode = "200", description = "수정 완료")
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_UPDATE")
	SuccessResponse<Void> updateSubgroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@Parameter(description = "소모임 ID", example = "301") @PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId,
		SubgroupUpdateRequest request);

	@Operation(summary = "소모임 가입", description = "소모임에 가입합니다. 비공개 소모임인 경우 비밀번호를 포함합니다.")
	@RequestBody(required = false, content = @Content(schema = @Schema(implementation = SubgroupJoinRequest.class)))
	@ApiResponse(responseCode = "200", description = "가입 완료", content = @Content(schema = @Schema(implementation = SubgroupJoinResponse.class)))
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_JOIN")
	SuccessResponse<SubgroupJoinResponse> joinSubgroup(
		@Parameter(description = "그룹 ID", example = "101") @PathVariable @Positive
		Long groupId,
		@Parameter(description = "소모임 ID", example = "301") @PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId,
		SubgroupJoinRequest request);
}
