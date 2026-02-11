package com.tasteam.domain.promotion.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.promotion.dto.PromotionDetailDto;
import com.tasteam.domain.promotion.dto.request.PromotionSearchRequest;
import com.tasteam.domain.promotion.dto.response.PromotionDetailResponse;
import com.tasteam.domain.promotion.dto.response.PromotionSummaryResponse;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;
import com.tasteam.domain.promotion.repository.PromotionRepository;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.PromotionErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionService {

	private final PromotionRepository promotionRepository;

	@Transactional(readOnly = true)
	public OffsetPageResponse<PromotionSummaryResponse> getPromotionList(PromotionSearchRequest request,
		Pageable pageable) {
		Page<PromotionSummaryResponse> result = promotionRepository
			.findDisplayingPromotions(pageable, request.promotionStatus())
			.map(PromotionSummaryResponse::fromDto);

		return new OffsetPageResponse<>(
			result.getContent(),
			new com.tasteam.global.dto.pagination.OffsetPagination(
				result.getNumber(),
				result.getSize(),
				result.getTotalPages(),
				(int)result.getTotalElements()));
	}

	@Transactional(readOnly = true)
	public PromotionDetailResponse getPromotionDetail(Long promotionId) {
		PromotionDetailDto dto = promotionRepository
			.findDisplayingPromotionById(promotionId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND));

		return PromotionDetailResponse.fromDto(dto);
	}

	@Transactional(readOnly = true)
	public Optional<SplashPromotionResponse> getSplashPromotion() {
		return promotionRepository
			.findSplashPromotion()
			.map(SplashPromotionResponse::fromDto);
	}

	@Transactional(readOnly = true)
	public OffsetPageResponse<PromotionSummaryResponse> getBannerPromotions(Pageable pageable) {
		Page<PromotionSummaryResponse> result = promotionRepository
			.findBannerPromotions(pageable)
			.map(PromotionSummaryResponse::fromDto);

		return new OffsetPageResponse<>(
			result.getContent(),
			new com.tasteam.global.dto.pagination.OffsetPagination(
				result.getNumber(),
				result.getSize(),
				result.getTotalPages(),
				(int)result.getTotalElements()));
	}
}
