package com.tasteam.domain.member.controller.docs;

import java.util.List;

import com.tasteam.domain.member.dto.request.MemberProfileUpdateRequest;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberMeResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.SwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Member", description = "회원 마이페이지 API")
public interface MemberControllerDocs {

	@Operation(summary = "마이페이지 회원 정보 조회", description = "현재 로그인 사용자의 프로필 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = MemberMeResponse.class)))
	@CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_ME)
	SuccessResponse<MemberMeResponse> getMyMemberInfo(
		@CurrentUser
		Long memberId);

	@Operation(summary = "내 그룹 요약 목록 조회", description = "현재 로그인 사용자의 그룹과 가입한 하위 그룹 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = MemberGroupSummaryResponse.class)))
	@CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_ME)
	SuccessResponse<List<MemberGroupSummaryResponse>> getMyGroupSummaries(
		@CurrentUser
		Long memberId);

	@Operation(summary = "회원 정보 수정", description = "프로필 이미지와 이메일을 수정합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = MemberProfileUpdateRequest.class)))
	@ApiResponse(responseCode = "204", description = "수정 완료")
	@CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_PROFILE_UPDATE)
	SuccessResponse<Void> updateMyProfile(
		@CurrentUser
		Long memberId,
		MemberProfileUpdateRequest request);

	@Operation(summary = "회원 탈퇴", description = "현재 로그인 사용자의 계정을 탈퇴 처리합니다.")
	@ApiResponse(responseCode = "204", description = "탈퇴 완료")
	@CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_WITHDRAW)
	SuccessResponse<Void> withdraw(
		@CurrentUser
		Long memberId);
}
