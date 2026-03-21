package com.tasteam.domain.promotion.controller.docs;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.promotion.dto.request.PromotionSearchRequest;
import com.tasteam.domain.promotion.dto.response.PromotionDetailResponse;
import com.tasteam.domain.promotion.dto.response.PromotionSummaryResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(65)
@Tag(name = "Promotion", description = "프로모션 API")
public interface PromotionControllerDocs {

	@Operation(summary = "프로모션 목록 조회", description = "노출 중인 프로모션 목록을 조회합니다. 프로모션 상태(eventStatus)로 필터링할 수 있습니다.")
	SuccessResponse<OffsetPageResponse<PromotionSummaryResponse>> getPromotionList(
		@ModelAttribute
		PromotionSearchRequest request,
		@PageableDefault(size = 20) @Parameter(hidden = true)
		Pageable pageable);

	@Operation(summary = "프로모션 상세 조회", description = "특정 프로모션의 상세 정보를 조회합니다. 노출 조건을 만족하지 않는 프로모션는 조회할 수 없습니다.")
	SuccessResponse<PromotionDetailResponse> getPromotionDetail(
		@Parameter(description = "프로모션 ID", example = "1") @PathVariable
		Long promotionId);
}
