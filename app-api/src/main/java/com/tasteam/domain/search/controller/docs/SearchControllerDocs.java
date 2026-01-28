package com.tasteam.domain.search.controller.docs;

import org.springdoc.core.annotations.ParameterObject;

import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Search", description = "검색 API")
public interface SearchControllerDocs {

	@Operation(summary = "검색", description = "키워드 기반으로 음식점/리뷰를 검색합니다. 요청 파라미터는 query string으로 전달합니다.")
	@ApiResponse(responseCode = "200", description = "검색 성공", content = @Content(schema = @Schema(implementation = SearchResponse.class)))
	SuccessResponse<SearchResponse> search(
		@CurrentUser
		Long memberId,
		@Valid @ParameterObject
		SearchRequest request);
}
