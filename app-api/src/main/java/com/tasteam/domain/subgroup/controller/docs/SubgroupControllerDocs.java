package com.tasteam.domain.subgroup.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.global.dto.api.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Subgroup", description = "소모임 리뷰 조회 API")
public interface SubgroupControllerDocs {

	@Operation(summary = "소모임 리뷰 목록 조회", description = "소모임에 속한 리뷰를 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = CursorPageResponse.class)))
	SuccessResponse<CursorPageResponse<ReviewResponse>> getSubgroupReviews(
		@Parameter(description = "소모임 ID", example = "301") @PathVariable
		Long subgroupId,
		@ParameterObject
		RestaurantReviewListRequest request);
}
