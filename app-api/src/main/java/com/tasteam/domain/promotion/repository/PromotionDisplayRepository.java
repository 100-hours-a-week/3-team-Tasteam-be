package com.tasteam.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.promotion.entity.PromotionDisplay;

public interface PromotionDisplayRepository extends JpaRepository<PromotionDisplay, Long> {

	Optional<PromotionDisplay> findByPromotionIdAndDeletedAtIsNull(Long promotionId);
}
