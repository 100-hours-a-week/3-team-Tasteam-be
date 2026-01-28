package com.tasteam.domain.search.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.search.dto.response.RecentSearchItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Search", description = "최근 검색어 API")
public interface RecentSearchControllerDocs {

	@Operation(summary = "최근 검색어 조회", description = "현재 로그인 사용자의 최근 검색어 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = OffsetPageResponse.class)))
	SuccessResponse<OffsetPageResponse<RecentSearchItem>> getRecentSearches(
		@CurrentUser
		Long memberId);

	@Operation(summary = "최근 검색어 삭제", description = "최근 검색어를 삭제합니다.")
	@ApiResponse(responseCode = "204", description = "삭제 완료")
	ResponseEntity<Void> deleteRecentSearch(
		@CurrentUser
		Long memberId,
		@Parameter(description = "최근 검색어 ID", example = "1") @PathVariable
		Long id);
}
