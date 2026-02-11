package com.tasteam.domain.promotion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.promotion.entity.AssetType;
import com.tasteam.domain.promotion.entity.PromotionAsset;

public interface PromotionAssetRepository extends JpaRepository<PromotionAsset, Long> {

	List<PromotionAsset> findByPromotionIdAndAssetTypeAndDeletedAtIsNullOrderBySortOrder(Long promotionId,
		AssetType assetType);

	Optional<PromotionAsset> findByPromotionIdAndAssetTypeAndIsPrimaryTrueAndDeletedAtIsNull(Long promotionId,
		AssetType assetType);
}
