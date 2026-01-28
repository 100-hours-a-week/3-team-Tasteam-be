package com.tasteam.domain.main.controller.docs;

import org.springdoc.core.annotations.ParameterObject;

import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Main", description = "메인 페이지 API")
public interface MainControllerDocs {

	@Operation(summary = "메인 페이지 조회", description = "현재 사용자 위치를 기준으로 메인 페이지 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = MainPageResponse.class)))
	SuccessResponse<MainPageResponse> getMain(
		@CurrentUser
		Long memberId,
		@ParameterObject
		MainPageRequest request);
}
