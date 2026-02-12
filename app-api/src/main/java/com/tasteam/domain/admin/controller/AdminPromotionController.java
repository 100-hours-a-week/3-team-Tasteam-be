package com.tasteam.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.dto.request.AdminPromotionCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminPromotionUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminPromotionDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminPromotionListItem;
import com.tasteam.domain.admin.service.AdminPromotionService;
import com.tasteam.domain.promotion.entity.DisplayStatus;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/promotions")
public class AdminPromotionController {

	private final AdminPromotionService adminPromotionService;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Page<AdminPromotionListItem>> getPromotions(
		@RequestParam(required = false)
		PromotionStatus promotionStatus,
		@RequestParam(required = false)
		PublishStatus publishStatus,
		@RequestParam(required = false)
		DisplayStatus displayStatus,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable) {

		Page<AdminPromotionListItem> result = adminPromotionService.getPromotionList(
			promotionStatus, publishStatus, displayStatus, pageable);
		return SuccessResponse.success(result);
	}

	@GetMapping("/{promotionId}")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminPromotionDetailResponse> getPromotion(
		@PathVariable
		Long promotionId) {

		AdminPromotionDetailResponse result = adminPromotionService.getPromotionDetail(promotionId);
		return SuccessResponse.success(result);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Long> createPromotion(
		@RequestBody @Validated
		AdminPromotionCreateRequest request) {

		Long promotionId = adminPromotionService.createPromotion(request);
		return SuccessResponse.success(promotionId);
	}

	@PatchMapping("/{promotionId}")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Void> updatePromotion(
		@PathVariable
		Long promotionId,
		@RequestBody @Validated
		AdminPromotionUpdateRequest request) {

		adminPromotionService.updatePromotion(promotionId, request);
		return SuccessResponse.success();
	}

	@DeleteMapping("/{promotionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePromotion(
		@PathVariable
		Long promotionId) {

		adminPromotionService.deletePromotion(promotionId);
	}
}
