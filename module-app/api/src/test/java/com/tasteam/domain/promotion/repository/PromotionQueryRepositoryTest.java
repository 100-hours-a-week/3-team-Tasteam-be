package com.tasteam.domain.promotion.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.promotion.dto.PromotionSummaryDto;
import com.tasteam.domain.promotion.entity.AssetType;
import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.Promotion;
import com.tasteam.domain.promotion.entity.PromotionAsset;
import com.tasteam.domain.promotion.entity.PromotionDisplay;
import com.tasteam.domain.promotion.entity.PromotionStatus;
import com.tasteam.domain.promotion.entity.PublishStatus;

@RepositoryJpaTest
@DisplayName("[유닛](Promotion) PromotionQueryRepository 단위 테스트")
class PromotionQueryRepositoryTest {

	private static final Instant PAST = Instant.parse("2000-01-01T00:00:00Z");
	private static final Instant FUTURE = Instant.parse("2999-01-01T00:00:00Z");
	private static final Instant NEAR_PAST = Instant.parse("2024-01-01T00:00:00Z");
	private static final Instant NEAR_FUTURE = Instant.parse("2027-01-01T00:00:00Z");

	@Autowired
	private PromotionRepository promotionRepository;

	@Autowired
	private PromotionDisplayRepository promotionDisplayRepository;

	@Autowired
	private PromotionAssetRepository promotionAssetRepository;

	// ======================== findSplashPromotion ========================

	@Test
	@DisplayName("스플래시 이미지가 없으면 배너 이미지를 fallback 으로 조회한다")
	void findSplashPromotion_usesBannerFallbackWhenSplashMissing() {
		Promotion promotion = saveDisplayingPromotion("배너 fallback 테스트", PAST, FUTURE, DisplayChannel.MAIN_BANNER);
		String bannerUrl = "https://example.com/banner-fallback.webp";
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.BANNER, bannerUrl, "배너 대체 텍스트", 0, true));

		var result = promotionRepository.findSplashPromotion();

		assertThat(result).isPresent();
		assertThat(result.get().thumbnailImageUrl()).isEqualTo(bannerUrl);
		assertThat(result.get().title()).isEqualTo("배너 fallback 테스트");
	}

	@Test
	@DisplayName("스플래시 이미지가 있으면 스플래시 이미지를 우선 조회한다")
	void findSplashPromotion_prefersSplashImageWhenPresent() {
		Promotion promotion = saveDisplayingPromotion("스플래시 우선 테스트", PAST, FUTURE, DisplayChannel.MAIN_BANNER);
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.BANNER, "https://example.com/banner.webp", "배너", 0, true));
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.SPLASH, "https://example.com/splash.webp", null, 0, true));

		var result = promotionRepository.findSplashPromotion();

		assertThat(result).isPresent();
		assertThat(result.get().thumbnailImageUrl()).isEqualTo("https://example.com/splash.webp");
	}

	@Test
	@DisplayName("displayEnabled=false 인 프로모션은 스플래시 조회에서 제외된다")
	void findSplashPromotion_excludesDisabledDisplay() {
		Promotion promotion = promotionRepository.save(
			Promotion.create("비활성 테스트", "본문", null, PAST, FUTURE, PublishStatus.PUBLISHED));
		PromotionDisplay display = PromotionDisplay.create(
			promotion, false, PAST, FUTURE, DisplayChannel.MAIN_BANNER, 0);
		promotionDisplayRepository.save(display);

		var result = promotionRepository.findSplashPromotion();

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("displayChannel=PROMOTION_LIST 인 프로모션은 스플래시 조회에서 제외된다")
	void findSplashPromotion_excludesPromotionListChannel() {
		saveDisplayingPromotion("리스트 채널", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);

		var result = promotionRepository.findSplashPromotion();

		assertThat(result).isEmpty();
	}

	// ======================== findDisplayingPromotions ========================

	@Test
	@DisplayName("ONGOING 필터는 현재 진행 중인 프로모션만 반환한다")
	void findDisplayingPromotions_ongoingFilter() {
		saveDisplayingPromotion("진행중", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		saveDisplayingPromotion("예정", NEAR_FUTURE, FUTURE, DisplayChannel.PROMOTION_LIST);
		saveDisplayingPromotion("종료", PAST, NEAR_PAST, DisplayChannel.PROMOTION_LIST);

		Page<PromotionSummaryDto> page = promotionRepository.findDisplayingPromotions(
			PageRequest.of(0, 10), PromotionStatus.ONGOING);

		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getContent().getFirst().title()).isEqualTo("진행중");
	}

	@Test
	@DisplayName("UPCOMING 필터는 시작 전 프로모션만 반환한다")
	void findDisplayingPromotions_upcomingFilter() {
		saveDisplayingPromotion("진행중", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		saveDisplayingPromotion("예정", NEAR_FUTURE, FUTURE, DisplayChannel.PROMOTION_LIST);

		Page<PromotionSummaryDto> page = promotionRepository.findDisplayingPromotions(
			PageRequest.of(0, 10), PromotionStatus.UPCOMING);

		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getContent().getFirst().title()).isEqualTo("예정");
	}

	@Test
	@DisplayName("ENDED 필터는 종료된 프로모션만 반환한다")
	void findDisplayingPromotions_endedFilter() {
		saveDisplayingPromotion("진행중", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		saveDisplayingPromotion("종료", PAST, NEAR_PAST, DisplayChannel.PROMOTION_LIST);

		Page<PromotionSummaryDto> page = promotionRepository.findDisplayingPromotions(
			PageRequest.of(0, 10), PromotionStatus.ENDED);

		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getContent().getFirst().title()).isEqualTo("종료");
	}

	@Test
	@DisplayName("status 필터가 null 이면 모든 표시 중인 프로모션을 반환한다")
	void findDisplayingPromotions_nullStatusReturnsAll() {
		saveDisplayingPromotion("진행중", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		saveDisplayingPromotion("예정", NEAR_FUTURE, FUTURE, DisplayChannel.PROMOTION_LIST);

		Page<PromotionSummaryDto> page = promotionRepository.findDisplayingPromotions(
			PageRequest.of(0, 10), null);

		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	@DisplayName("DRAFT 상태 프로모션은 조회에서 제외된다")
	void findDisplayingPromotions_excludesDraft() {
		Promotion draft = promotionRepository.save(
			Promotion.create("초안", "본문", null, PAST, FUTURE, PublishStatus.DRAFT));
		promotionDisplayRepository.save(
			PromotionDisplay.create(draft, true, PAST, FUTURE, DisplayChannel.PROMOTION_LIST, 0));

		Page<PromotionSummaryDto> page = promotionRepository.findDisplayingPromotions(
			PageRequest.of(0, 10), null);

		assertThat(page.getContent()).isEmpty();
	}

	// ======================== findBannerPromotions ========================

	@Test
	@DisplayName("배너 조회는 MAIN_BANNER 채널과 BOTH 채널 프로모션을 모두 반환한다")
	void findBannerPromotions_includesMainBannerAndBothChannels() {
		saveDisplayingPromotion("메인배너", PAST, FUTURE, DisplayChannel.MAIN_BANNER);
		saveDisplayingPromotion("BOTH", PAST, FUTURE, DisplayChannel.BOTH);
		saveDisplayingPromotion("리스트전용", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);

		Page<PromotionSummaryDto> page = promotionRepository.findBannerPromotions(PageRequest.of(0, 10));

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent())
			.extracting(PromotionSummaryDto::title)
			.containsExactlyInAnyOrder("메인배너", "BOTH");
	}

	@Test
	@DisplayName("배너 조회는 displayPriority 오름차순으로 정렬된다")
	void findBannerPromotions_orderedByDisplayPriority() {
		Promotion low = promotionRepository.save(
			Promotion.create("낮은우선순위", "본문", null, PAST, FUTURE, PublishStatus.PUBLISHED));
		promotionDisplayRepository.save(
			PromotionDisplay.create(low, true, PAST, FUTURE, DisplayChannel.MAIN_BANNER, 10));

		Promotion high = promotionRepository.save(
			Promotion.create("높은우선순위", "본문", null, PAST, FUTURE, PublishStatus.PUBLISHED));
		promotionDisplayRepository.save(
			PromotionDisplay.create(high, true, PAST, FUTURE, DisplayChannel.MAIN_BANNER, 1));

		Page<PromotionSummaryDto> page = promotionRepository.findBannerPromotions(PageRequest.of(0, 10));

		assertThat(page.getContent().getFirst().title()).isEqualTo("높은우선순위");
		assertThat(page.getContent().getLast().title()).isEqualTo("낮은우선순위");
	}

	// ======================== findDetailImageUrls ========================

	@Test
	@DisplayName("상세 이미지는 sortOrder 오름차순으로 반환된다")
	void findDetailImageUrls_orderedBySortOrder() {
		Promotion promotion = saveDisplayingPromotion("이미지 정렬", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.DETAIL, "https://example.com/detail-2.webp", null, 2, false));
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.DETAIL, "https://example.com/detail-1.webp", null, 1, false));
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.DETAIL, "https://example.com/detail-0.webp", null, 0, false));

		List<String> urls = promotionRepository.findDetailImageUrls(promotion.getId());

		assertThat(urls).containsExactly(
			"https://example.com/detail-0.webp",
			"https://example.com/detail-1.webp",
			"https://example.com/detail-2.webp");
	}

	@Test
	@DisplayName("deletedAt 이 설정된 상세 이미지는 조회에서 제외된다")
	void findDetailImageUrls_excludesDeletedAssets() {
		Promotion promotion = saveDisplayingPromotion("삭제 이미지", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		PromotionAsset active = promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.DETAIL, "https://example.com/active.webp", null, 0, false));
		PromotionAsset deleted = promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.DETAIL, "https://example.com/deleted.webp", null, 1, false));
		deleted.delete(Instant.now());
		promotionAssetRepository.save(deleted);

		List<String> urls = promotionRepository.findDetailImageUrls(promotion.getId());

		assertThat(urls).containsExactly("https://example.com/active.webp");
	}

	// ======================== findDetailImageUrlsByIds ========================

	@Test
	@DisplayName("bulk 조회는 여러 프로모션의 상세 이미지를 promotionId 별로 그룹핑해 반환한다")
	void findDetailImageUrlsByIds_groupsByPromotionId() {
		Promotion p1 = saveDisplayingPromotion("프로모션1", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		Promotion p2 = saveDisplayingPromotion("프로모션2", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);

		promotionAssetRepository.save(
			PromotionAsset.create(p1, AssetType.DETAIL, "https://example.com/p1-a.webp", null, 0, false));
		promotionAssetRepository.save(
			PromotionAsset.create(p1, AssetType.DETAIL, "https://example.com/p1-b.webp", null, 1, false));
		promotionAssetRepository.save(
			PromotionAsset.create(p2, AssetType.DETAIL, "https://example.com/p2-a.webp", null, 0, false));

		Map<Long, List<String>> result = promotionRepository.findDetailImageUrlsByIds(
			List.of(p1.getId(), p2.getId()));

		assertThat(result.get(p1.getId())).containsExactly(
			"https://example.com/p1-a.webp",
			"https://example.com/p1-b.webp");
		assertThat(result.get(p2.getId())).containsExactly("https://example.com/p2-a.webp");
	}

	@Test
	@DisplayName("빈 id 목록으로 bulk 조회 시 빈 Map을 반환한다")
	void findDetailImageUrlsByIds_emptyInput_returnsEmptyMap() {
		Map<Long, List<String>> result = promotionRepository.findDetailImageUrlsByIds(List.of());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("bulk 조회 시 DETAIL 타입이 아닌 이미지는 제외된다")
	void findDetailImageUrlsByIds_excludesNonDetailAssets() {
		Promotion promotion = saveDisplayingPromotion("배너 제외", PAST, FUTURE, DisplayChannel.PROMOTION_LIST);
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.BANNER, "https://example.com/banner.webp", null, 0, true));
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.DETAIL, "https://example.com/detail.webp", null, 0, false));

		Map<Long, List<String>> result = promotionRepository.findDetailImageUrlsByIds(List.of(promotion.getId()));

		assertThat(result.get(promotion.getId())).containsExactly("https://example.com/detail.webp");
	}

	// ======================== helpers ========================

	private Promotion saveDisplayingPromotion(
		String title, Instant promotionStart, Instant promotionEnd, DisplayChannel channel) {
		Promotion promotion = promotionRepository.save(
			Promotion.create(title, "본문", "https://example.com/landing", promotionStart, promotionEnd,
				PublishStatus.PUBLISHED));
		promotionDisplayRepository.save(
			PromotionDisplay.create(promotion, true, PAST, FUTURE, channel, 0));
		return promotion;
	}
}
