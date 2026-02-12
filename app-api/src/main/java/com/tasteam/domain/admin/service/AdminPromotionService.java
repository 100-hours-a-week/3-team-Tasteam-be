package com.tasteam.domain.admin.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.request.AdminPromotionCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminPromotionUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminPromotionDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminPromotionListItem;
import com.tasteam.domain.promotion.entity.AssetType;
import com.tasteam.domain.promotion.entity.DisplayStatus;
import com.tasteam.domain.promotion.entity.Promotion;
import com.tasteam.domain.promotion.entity.PromotionAsset;
import com.tasteam.domain.promotion.entity.PromotionDisplay;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;
import com.tasteam.domain.promotion.repository.PromotionAssetRepository;
import com.tasteam.domain.promotion.repository.PromotionDisplayRepository;
import com.tasteam.domain.promotion.repository.PromotionRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.PromotionErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPromotionService {

	private final PromotionRepository promotionRepository;
	private final PromotionDisplayRepository promotionDisplayRepository;
	private final PromotionAssetRepository promotionAssetRepository;

	@Transactional
	public Long createPromotion(AdminPromotionCreateRequest request) {
		validatePromotionPeriod(request.promotionStartAt(), request.promotionEndAt());
		validateDisplayPeriod(request.displayStartAt(), request.displayEndAt());

		Promotion promotion = Promotion.create(
			request.title(),
			request.content(),
			request.landingUrl(),
			request.promotionStartAt(),
			request.promotionEndAt(),
			request.publishStatus());
		promotionRepository.save(promotion);

		PromotionDisplay display = PromotionDisplay.create(
			promotion,
			request.displayEnabled(),
			request.displayStartAt(),
			request.displayEndAt(),
			request.displayChannel(),
			request.displayPriority());
		promotionDisplayRepository.save(display);

		PromotionAsset banner = PromotionAsset.create(
			promotion,
			AssetType.BANNER,
			request.bannerImageUrl(),
			request.bannerImageAltText(),
			0,
			true);
		promotionAssetRepository.save(banner);

		if (request.detailImageUrls() != null && !request.detailImageUrls().isEmpty()) {
			List<PromotionAsset> detailAssets = new ArrayList<>();
			for (int i = 0; i < request.detailImageUrls().size(); i++) {
				PromotionAsset detail = PromotionAsset.create(
					promotion,
					AssetType.DETAIL,
					request.detailImageUrls().get(i),
					null,
					i,
					false);
				detailAssets.add(detail);
			}
			promotionAssetRepository.saveAll(detailAssets);
		}

		return promotion.getId();
	}

	@Transactional(readOnly = true)
	public Page<AdminPromotionListItem> getPromotionList(
		PromotionStatus promotionStatus,
		PublishStatus publishStatus,
		DisplayStatus displayStatus,
		Pageable pageable) {

		Page<Promotion> promotions = promotionRepository.findByDeletedAtIsNull(pageable);

		List<AdminPromotionListItem> items = promotions.getContent().stream()
			.map(promotion -> {
				PromotionDisplay display = promotionDisplayRepository
					.findByPromotionIdAndDeletedAtIsNull(promotion.getId())
					.orElse(null);

				if (display == null) {
					return null;
				}

				PromotionStatus currentPromotionStatus = promotion.getPromotionStatus();
				DisplayStatus currentDisplayStatus = display.getDisplayStatus();

				if (promotionStatus != null && currentPromotionStatus != promotionStatus) {
					return null;
				}
				if (publishStatus != null && promotion.getPublishStatus() != publishStatus) {
					return null;
				}
				if (displayStatus != null && currentDisplayStatus != displayStatus) {
					return null;
				}

				PromotionAsset bannerAsset = promotionAssetRepository
					.findByPromotionIdAndAssetTypeAndIsPrimaryTrueAndDeletedAtIsNull(
						promotion.getId(), AssetType.BANNER)
					.orElse(null);

				return new AdminPromotionListItem(
					promotion.getId(),
					promotion.getTitle(),
					currentPromotionStatus,
					currentDisplayStatus,
					promotion.getPublishStatus(),
					promotion.getPromotionStartAt(),
					promotion.getPromotionEndAt(),
					display.getDisplayChannel(),
					bannerAsset != null ? bannerAsset.getImageUrl() : null,
					promotion.getCreatedAt());
			})
			.filter(item -> item != null)
			.toList();

		return new PageImpl<>(items, pageable, promotions.getTotalElements());
	}

	@Transactional(readOnly = true)
	public AdminPromotionDetailResponse getPromotionDetail(Long promotionId) {
		Promotion promotion = promotionRepository.findByIdAndDeletedAtIsNull(promotionId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND));

		PromotionDisplay display = promotionDisplayRepository
			.findByPromotionIdAndDeletedAtIsNull(promotionId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND,
				"프로모션 노출 정보를 찾을 수 없습니다"));

		PromotionAsset bannerAsset = promotionAssetRepository
			.findByPromotionIdAndAssetTypeAndIsPrimaryTrueAndDeletedAtIsNull(
				promotionId, AssetType.BANNER)
			.orElse(null);

		List<String> detailImageUrls = promotionAssetRepository
			.findByPromotionIdAndAssetTypeAndDeletedAtIsNullOrderBySortOrder(
				promotionId, AssetType.DETAIL)
			.stream()
			.map(PromotionAsset::getImageUrl)
			.toList();

		return new AdminPromotionDetailResponse(
			promotion.getId(),
			promotion.getTitle(),
			promotion.getContent(),
			promotion.getLandingUrl(),
			promotion.getPromotionStartAt(),
			promotion.getPromotionEndAt(),
			promotion.getPublishStatus(),
			promotion.getPromotionStatus(),
			display.isDisplayEnabled(),
			display.getDisplayStartAt(),
			display.getDisplayEndAt(),
			display.getDisplayChannel(),
			display.getDisplayPriority(),
			display.getDisplayStatus(),
			bannerAsset != null ? bannerAsset.getImageUrl() : null,
			bannerAsset != null ? bannerAsset.getAltText() : null,
			detailImageUrls,
			promotion.getCreatedAt(),
			promotion.getUpdatedAt());
	}

	@Transactional
	public void updatePromotion(Long promotionId, AdminPromotionUpdateRequest request) {
		Promotion promotion = promotionRepository.findByIdAndDeletedAtIsNull(promotionId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND));

		PromotionDisplay display = promotionDisplayRepository
			.findByPromotionIdAndDeletedAtIsNull(promotionId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND,
				"프로모션 노출 정보를 찾을 수 없습니다"));

		if (request.title() != null || request.content() != null || request.landingUrl() != null) {
			promotion.changeBasicInfo(request.title(), request.content(), request.landingUrl());
		}

		if (request.promotionStartAt() != null || request.promotionEndAt() != null) {
			Instant newStartAt = request.promotionStartAt() != null ? request.promotionStartAt()
				: promotion.getPromotionStartAt();
			Instant newEndAt = request.promotionEndAt() != null ? request.promotionEndAt()
				: promotion.getPromotionEndAt();
			validatePromotionPeriod(newStartAt, newEndAt);
			promotion.changeSchedule(request.promotionStartAt(), request.promotionEndAt());
		}

		if (request.publishStatus() != null) {
			promotion.changePublishStatus(request.publishStatus());
		}

		if (request.displayStartAt() != null || request.displayEndAt() != null) {
			Instant newDisplayStartAt = request.displayStartAt() != null ? request.displayStartAt()
				: display.getDisplayStartAt();
			Instant newDisplayEndAt = request.displayEndAt() != null ? request.displayEndAt()
				: display.getDisplayEndAt();
			validateDisplayPeriod(newDisplayStartAt, newDisplayEndAt);
		}

		display.changeDisplaySettings(
			request.displayEnabled(),
			request.displayStartAt(),
			request.displayEndAt(),
			request.displayChannel(),
			request.displayPriority());

		if (request.bannerImageUrl() != null) {
			List<PromotionAsset> oldBanners = promotionAssetRepository
				.findByPromotionIdAndAssetTypeAndDeletedAtIsNullOrderBySortOrder(
					promotionId, AssetType.BANNER);
			oldBanners.forEach(asset -> asset.delete(Instant.now()));

			PromotionAsset newBanner = PromotionAsset.create(
				promotion,
				AssetType.BANNER,
				request.bannerImageUrl(),
				request.bannerImageAltText(),
				0,
				true);
			promotionAssetRepository.save(newBanner);
		}

		if (request.detailImageUrls() != null) {
			List<PromotionAsset> oldDetails = promotionAssetRepository
				.findByPromotionIdAndAssetTypeAndDeletedAtIsNullOrderBySortOrder(
					promotionId, AssetType.DETAIL);
			oldDetails.forEach(asset -> asset.delete(Instant.now()));

			if (!request.detailImageUrls().isEmpty()) {
				List<PromotionAsset> newDetails = new ArrayList<>();
				for (int i = 0; i < request.detailImageUrls().size(); i++) {
					PromotionAsset detail = PromotionAsset.create(
						promotion,
						AssetType.DETAIL,
						request.detailImageUrls().get(i),
						null,
						i,
						false);
					newDetails.add(detail);
				}
				promotionAssetRepository.saveAll(newDetails);
			}
		}
	}

	@Transactional
	public void deletePromotion(Long promotionId) {
		Instant now = Instant.now();

		Promotion promotion = promotionRepository.findByIdAndDeletedAtIsNull(promotionId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.PROMOTION_NOT_FOUND));
		promotion.delete(now);

		promotionDisplayRepository.findByPromotionIdAndDeletedAtIsNull(promotionId)
			.ifPresent(display -> display.delete(now));

		List<PromotionAsset> assets = promotionAssetRepository
			.findByPromotionIdAndDeletedAtIsNull(promotionId);
		assets.forEach(asset -> asset.delete(now));
	}

	private void validatePromotionPeriod(Instant startAt, Instant endAt) {
		if (startAt.isAfter(endAt)) {
			throw new BusinessException(
				PromotionErrorCode.INVALID_PROMOTION_PERIOD,
				"프로모션 시작일은 종료일보다 이전이어야 합니다");
		}
	}

	private void validateDisplayPeriod(Instant startAt, Instant endAt) {
		if (startAt.isAfter(endAt)) {
			throw new BusinessException(
				PromotionErrorCode.INVALID_DISPLAY_PERIOD,
				"노출 시작일은 종료일보다 이전이어야 합니다");
		}
	}
}
