package com.tasteam.domain.subgroup.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.subgroup.dto.SubgroupChatRoomResponse;
import com.tasteam.domain.subgroup.dto.SubgroupDetailResponse;
import com.tasteam.domain.subgroup.dto.SubgroupMemberListItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.subgroup.SubgroupSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Tag(name = "Subgroup", description = "소모임 조회 API")
public interface SubgroupControllerDocs {

	@Operation(summary = "소모임 리뷰 목록 조회", description = "소모임에 속한 리뷰를 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	SuccessResponse<CursorPageResponse<ReviewResponse>> getSubgroupReviews(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable @Positive
		Long subgroupId,
		@Validated @ParameterObject
		RestaurantReviewListRequest request);

	@Operation(summary = "소모임 멤버 목록 조회", description = "소모임 멤버를 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_MEMBER_LIST")
	SuccessResponse<CursorPageResponse<SubgroupMemberListItem>> getSubgroupMembers(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable @Positive
		Long subgroupId,
		@Parameter(description = "페이징 커서", example = "cursor") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false)
		Integer size,
		@CurrentUser
		Long memberId);

	@Operation(summary = "소모임 상세 조회", description = "소모임 ID로 상세 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = SubgroupDetailResponse.class)))
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_DETAIL")
	SuccessResponse<SubgroupDetailResponse> getSubgroup(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId);

	@Operation(summary = "소모임 채팅방 조회", description = "소모임 ID로 채팅방 ID를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = SubgroupChatRoomResponse.class)))
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_CHAT_ROOM")
	SuccessResponse<SubgroupChatRoomResponse> getChatRoom(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId);

	@Operation(summary = "소모임 탈퇴", description = "현재 로그인 사용자가 소모임에서 탈퇴합니다.")
	@ApiResponse(responseCode = "204", description = "탈퇴 완료")
	@CustomErrorResponseDescription(value = SubgroupSwaggerErrorResponseDescription.class, group = "SUBGROUP_WITHDRAW")
	org.springframework.http.ResponseEntity<Void> withdrawSubgroup(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable @Positive
		Long subgroupId,
		@CurrentUser
		Long memberId);
}
