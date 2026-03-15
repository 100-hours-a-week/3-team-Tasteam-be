package com.tasteam.domain.promotion.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.promotion.dto.PromotionDetailDto;
import com.tasteam.domain.promotion.dto.PromotionSummaryDto;
import com.tasteam.domain.promotion.dto.request.PromotionSearchRequest;
import com.tasteam.domain.promotion.dto.response.PromotionDetailResponse;
import com.tasteam.domain.promotion.dto.response.PromotionSummaryResponse;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;
import com.tasteam.domain.promotion.repository.PromotionRepository;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;
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
		Page<PromotionSummaryDto> page = promotionRepository
			.findDisplayingPromotions(pageable, request.promotionStatus());
		List<Long> ids = page.getContent().stream().map(PromotionSummaryDto::promotionId).toList();
		Map<Long, List<String>> detailImages = promotionRepository.findDetailImageUrlsByIds(ids);

		List<PromotionSummaryResponse> items = page.getContent().stream()
			.map(dto -> PromotionSummaryResponse.fromDto(dto,
				detailImages.getOrDefault(dto.promotionId(), List.of())))
			.toList();

		return new OffsetPageResponse<>(items,
			new OffsetPagination(page.getNumber(), page.getSize(),
				page.getTotalPages(), (int)page.getTotalElements()));
	}

	@Transactional(readOnly = true)
	public PromotionDetailResponse getPromotionDetail(Long promotionId) {
		PromotionDetailDto dto = promotionRepository
			.findDisplayingPromotionById(promotionId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND));

		return PromotionDetailResponse.fromDto(dto);
	}

	@Cacheable(cacheNames = "main-splash", key = "'splash'")
	@Transactional(readOnly = true)
	public Optional<SplashPromotionResponse> getSplashPromotion() {
		return promotionRepository
			.findSplashPromotion()
			.map(dto -> SplashPromotionResponse.fromDto(dto,
				promotionRepository.findDetailImageUrls(dto.promotionId())));
	}

	@Cacheable(cacheNames = "main-banners", key = "'banners'")
	@Transactional(readOnly = true)
	public OffsetPageResponse<PromotionSummaryResponse> getBannerPromotions(Pageable pageable) {
		Page<PromotionSummaryDto> page = promotionRepository.findBannerPromotions(pageable);
		List<Long> ids = page.getContent().stream().map(PromotionSummaryDto::promotionId).toList();
		Map<Long, List<String>> detailImages = promotionRepository.findDetailImageUrlsByIds(ids);

		List<PromotionSummaryResponse> items = page.getContent().stream()
			.map(dto -> PromotionSummaryResponse.fromDto(dto,
				detailImages.getOrDefault(dto.promotionId(), List.of())))
			.toList();

		return new OffsetPageResponse<>(items,
			new OffsetPagination(page.getNumber(), page.getSize(),
				page.getTotalPages(), (int)page.getTotalElements()));
	}
}
