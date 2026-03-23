package com.tasteam.domain.promotion.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.promotion.controller.docs.PromotionControllerDocs;
import com.tasteam.domain.promotion.dto.request.PromotionSearchRequest;
import com.tasteam.domain.promotion.dto.response.PromotionDetailResponse;
import com.tasteam.domain.promotion.dto.response.PromotionSummaryResponse;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController implements PromotionControllerDocs {

	private final PromotionService promotionService;

	@Override
	@GetMapping
	public SuccessResponse<OffsetPageResponse<PromotionSummaryResponse>> getPromotionList(
		@ModelAttribute
		PromotionSearchRequest request,
		@PageableDefault(size = 20)
		Pageable pageable) {
		return SuccessResponse.success(promotionService.getPromotionList(request, pageable));
	}

	@Override
	@GetMapping("/{promotionId}")
	public SuccessResponse<PromotionDetailResponse> getPromotionDetail(@PathVariable
	Long promotionId) {
		return SuccessResponse.success(promotionService.getPromotionDetail(promotionId));
	}
}
