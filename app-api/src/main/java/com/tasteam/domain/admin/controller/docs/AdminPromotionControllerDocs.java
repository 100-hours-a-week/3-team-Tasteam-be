package com.tasteam.domain.admin.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.admin.dto.request.AdminPromotionCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminPromotionUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminPromotionDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminPromotionListItem;
import com.tasteam.domain.promotion.entity.DisplayStatus;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(140)
@Tag(name = "Admin - Promotion", description = "어드민 프로모션 관리 API")
public interface AdminPromotionControllerDocs {

	@Operation(summary = "프로모션 목록 조회", description = "필터 조건으로 프로모션 목록을 조회합니다.")
	SuccessResponse<Page<AdminPromotionListItem>> getPromotions(
		@Parameter(description = "프로모션 상태") @RequestParam(required = false)
		PromotionStatus promotionStatus,
		@Parameter(description = "게시 상태") @RequestParam(required = false)
		PublishStatus publishStatus,
		@Parameter(description = "노출 상태") @RequestParam(required = false)
		DisplayStatus displayStatus,
		Pageable pageable);

	@Operation(summary = "프로모션 상세 조회", description = "프로모션 ID로 상세 정보를 조회합니다.")
	SuccessResponse<AdminPromotionDetailResponse> getPromotion(
		@Parameter(description = "프로모션 ID", example = "1") @PathVariable
		Long promotionId);

	@Operation(summary = "프로모션 등록", description = "새 프로모션을 등록합니다.")
	SuccessResponse<Long> createPromotion(
		@RequestBody @Validated
		AdminPromotionCreateRequest request);

	@Operation(summary = "프로모션 수정", description = "프로모션 정보를 수정합니다.")
	SuccessResponse<Void> updatePromotion(
		@Parameter(description = "프로모션 ID", example = "1") @PathVariable
		Long promotionId,
		@RequestBody @Validated
		AdminPromotionUpdateRequest request);

	@Operation(summary = "프로모션 삭제", description = "프로모션을 삭제합니다.")
	void deletePromotion(
		@Parameter(description = "프로모션 ID", example = "1") @PathVariable
		Long promotionId);
}
