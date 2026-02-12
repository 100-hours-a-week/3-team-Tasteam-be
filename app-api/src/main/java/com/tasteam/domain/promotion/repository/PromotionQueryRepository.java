package com.tasteam.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tasteam.domain.promotion.dto.PromotionDetailDto;
import com.tasteam.domain.promotion.dto.PromotionSummaryDto;
import com.tasteam.domain.promotion.dto.SplashPromotionDto;
import com.tasteam.domain.promotion.entity.PromotionStatus;

public interface PromotionQueryRepository {

	Page<PromotionSummaryDto> findDisplayingPromotions(Pageable pageable, PromotionStatus promotionStatus);

	Optional<PromotionDetailDto> findDisplayingPromotionById(Long promotionId);

	Optional<SplashPromotionDto> findSplashPromotion();

	Page<PromotionSummaryDto> findBannerPromotions(Pageable pageable);
}
