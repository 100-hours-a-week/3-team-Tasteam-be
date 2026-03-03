package com.tasteam.domain.promotion.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.promotion.entity.AssetType;
import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.Promotion;
import com.tasteam.domain.promotion.entity.PromotionAsset;
import com.tasteam.domain.promotion.entity.PromotionDisplay;
import com.tasteam.domain.promotion.entity.PublishStatus;

@RepositoryJpaTest
@DisplayName("[유닛](Promotion) PromotionQueryRepository 단위 테스트")
class PromotionQueryRepositoryTest {

	@Autowired
	private PromotionRepository promotionRepository;

	@Autowired
	private PromotionDisplayRepository promotionDisplayRepository;

	@Autowired
	private PromotionAssetRepository promotionAssetRepository;

	@Test
	@DisplayName("스플래시 이미지가 없으면 배너 이미지를 fallback 으로 조회한다")
	void findSplashPromotion_usesBannerFallbackWhenSplashMissing() {
		Promotion promotion = saveDisplayingPromotion("배너 fallback 테스트");
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
		Promotion promotion = saveDisplayingPromotion("스플래시 우선 테스트");
		String bannerUrl = "https://example.com/banner.webp";
		String splashUrl = "https://example.com/splash.webp";

		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.BANNER, bannerUrl, "배너 대체 텍스트", 0, true));
		promotionAssetRepository.save(
			PromotionAsset.create(promotion, AssetType.SPLASH, splashUrl, null, 0, true));

		var result = promotionRepository.findSplashPromotion();

		assertThat(result).isPresent();
		assertThat(result.get().thumbnailImageUrl()).isEqualTo(splashUrl);
		assertThat(result.get().title()).isEqualTo("스플래시 우선 테스트");
	}

	private Promotion saveDisplayingPromotion(String title) {
		Instant activeStart = Instant.parse("2000-01-01T00:00:00Z");
		Instant activeEnd = Instant.parse("2999-01-01T00:00:00Z");
		Promotion promotion = promotionRepository.save(
			Promotion.create(
				title,
				"본문",
				"https://example.com/landing",
				activeStart,
				activeEnd,
				PublishStatus.PUBLISHED));

		PromotionDisplay display = PromotionDisplay.create(
			promotion,
			true,
			activeStart,
			activeEnd,
			DisplayChannel.MAIN_BANNER,
			-100);
		promotionDisplayRepository.save(display);

		return promotion;
	}
}
