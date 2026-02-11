package com.tasteam.domain.promotion.repository;

import static com.tasteam.domain.promotion.entity.QPromotion.*;
import static com.tasteam.domain.promotion.entity.QPromotionAsset.*;
import static com.tasteam.domain.promotion.entity.QPromotionDisplay.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.promotion.dto.PromotionDetailDto;
import com.tasteam.domain.promotion.dto.PromotionSummaryDto;
import com.tasteam.domain.promotion.dto.QPromotionSummaryDto;
import com.tasteam.domain.promotion.dto.QSplashPromotionDto;
import com.tasteam.domain.promotion.dto.SplashPromotionDto;
import com.tasteam.domain.promotion.entity.AssetType;
import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.Promotion;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;

@Repository
public class PromotionQueryRepositoryImpl extends QueryDslSupport implements PromotionQueryRepository {

	public PromotionQueryRepositoryImpl() {
		super(Promotion.class);
	}

	@Override
	public Page<PromotionSummaryDto> findDisplayingPromotions(Pageable pageable, PromotionStatus promotionStatus) {
		Instant now = Instant.now();

		JPAQuery<PromotionSummaryDto> contentQuery = getQueryFactory()
			.select(new QPromotionSummaryDto(
				promotion.id,
				promotion.title,
				promotion.content,
				promotion.landingUrl,
				promotion.promotionStartAt,
				promotion.promotionEndAt,
				promotion.publishStatus,
				promotionDisplay.displayEnabled,
				promotionDisplay.displayStartAt,
				promotionDisplay.displayEndAt,
				promotionDisplay.displayChannel,
				promotionAsset.imageUrl))
			.from(promotion)
			.join(promotionDisplay).on(promotionDisplay.promotion.eq(promotion))
			.leftJoin(promotionAsset).on(
				promotionAsset.promotion.eq(promotion)
					.and(promotionAsset.assetType.eq(AssetType.BANNER))
					.and(promotionAsset.isPrimary.isTrue())
					.and(promotionAsset.deletedAt.isNull()))
			.where(
				displayConditions(now),
				promotionStatusCondition(promotionStatus, now))
			.orderBy(promotion.id.desc());

		JPAQuery<Long> countQuery = getQueryFactory()
			.select(promotion.count())
			.from(promotion)
			.join(promotionDisplay).on(promotionDisplay.promotion.eq(promotion))
			.where(
				displayConditions(now),
				promotionStatusCondition(promotionStatus, now));

		return applyPagination(pageable, contentQuery, countQuery);
	}

	@Override
	public Optional<PromotionDetailDto> findDisplayingPromotionById(Long promotionId) {
		Instant now = Instant.now();

		PromotionSummaryDto summary = getQueryFactory()
			.select(new QPromotionSummaryDto(
				promotion.id,
				promotion.title,
				promotion.content,
				promotion.landingUrl,
				promotion.promotionStartAt,
				promotion.promotionEndAt,
				promotion.publishStatus,
				promotionDisplay.displayEnabled,
				promotionDisplay.displayStartAt,
				promotionDisplay.displayEndAt,
				promotionDisplay.displayChannel,
				promotionAsset.imageUrl))
			.from(promotion)
			.join(promotionDisplay).on(promotionDisplay.promotion.eq(promotion))
			.leftJoin(promotionAsset).on(
				promotionAsset.promotion.eq(promotion)
					.and(promotionAsset.assetType.eq(AssetType.BANNER))
					.and(promotionAsset.isPrimary.isTrue())
					.and(promotionAsset.deletedAt.isNull()))
			.where(
				promotion.id.eq(promotionId),
				displayConditions(now))
			.fetchOne();

		if (summary == null) {
			return Optional.empty();
		}

		List<String> detailImages = getQueryFactory()
			.select(promotionAsset.imageUrl)
			.from(promotionAsset)
			.where(
				promotionAsset.promotion.id.eq(promotionId),
				promotionAsset.assetType.eq(AssetType.DETAIL),
				promotionAsset.deletedAt.isNull())
			.orderBy(promotionAsset.sortOrder.asc())
			.fetch();

		return Optional.of(new PromotionDetailDto(
			summary.promotionId(),
			summary.title(),
			summary.content(),
			summary.landingUrl(),
			summary.promotionStartAt(),
			summary.promotionEndAt(),
			summary.publishStatus(),
			summary.displayEnabled(),
			summary.displayStartAt(),
			summary.displayEndAt(),
			summary.displayChannel(),
			summary.bannerImageUrl(),
			detailImages));
	}

	@Override
	public Optional<SplashPromotionDto> findSplashPromotion() {
		Instant now = Instant.now();

		SplashPromotionDto result = getQueryFactory()
			.select(new QSplashPromotionDto(
				promotion.id,
				promotion.title,
				promotion.content,
				promotionAsset.imageUrl,
				promotion.promotionStartAt,
				promotion.promotionEndAt))
			.from(promotion)
			.join(promotionDisplay).on(promotionDisplay.promotion.eq(promotion))
			.leftJoin(promotionAsset).on(
				promotionAsset.promotion.eq(promotion)
					.and(promotionAsset.assetType.eq(AssetType.BANNER))
					.and(promotionAsset.isPrimary.isTrue())
					.and(promotionAsset.deletedAt.isNull()))
			.where(
				displayConditions(now),
				splashChannelCondition())
			.orderBy(
				promotionDisplay.displayPriority.asc(),
				promotionDisplay.displayStartAt.desc(),
				promotion.id.desc())
			.limit(1)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	private BooleanExpression displayConditions(Instant now) {
		return nullSafeBuilder(
			promotion.deletedAt.isNull(),
			promotion.publishStatus.eq(PublishStatus.PUBLISHED),
			promotionDisplay.deletedAt.isNull(),
			promotionDisplay.displayEnabled.isTrue(),
			promotionDisplay.displayStartAt.loe(now),
			promotionDisplay.displayEndAt.goe(now));
	}

	private BooleanExpression promotionStatusCondition(PromotionStatus promotionStatus, Instant now) {
		if (promotionStatus == null) {
			return null;
		}

		return switch (promotionStatus) {
			case UPCOMING -> promotion.promotionStartAt.gt(now);
			case ONGOING -> promotion.promotionStartAt.loe(now).and(promotion.promotionEndAt.goe(now));
			case ENDED -> promotion.promotionEndAt.lt(now);
		};
	}

	private BooleanExpression splashChannelCondition() {
		return promotionDisplay.displayChannel.in(DisplayChannel.MAIN_BANNER, DisplayChannel.BOTH);
	}
}
