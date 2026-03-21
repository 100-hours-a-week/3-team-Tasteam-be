package com.tasteam.fixture;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.main.dto.response.HomePageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse.BannerItem;
import com.tasteam.domain.main.dto.response.MainPageResponse.Banners;
import com.tasteam.domain.main.dto.response.MainPageResponse.Section;
import com.tasteam.domain.main.dto.response.MainSectionItem;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;

public final class MainPageResponseFixture {

	public static final Long DEFAULT_RESTAURANT_ID = 1L;
	public static final String DEFAULT_RESTAURANT_NAME = "맛집1";
	public static final Double DEFAULT_DISTANCE = 100.0;
	public static final List<String> DEFAULT_CATEGORIES = List.of("한식", "국밥");
	public static final String DEFAULT_THUMBNAIL = "https://example.com/img1.jpg";
	public static final String DEFAULT_SUMMARY = "맛있어요";

	public static final Long DEFAULT_BANNER_ID = 10L;
	public static final String DEFAULT_BANNER_IMAGE = "https://example.com/banner.jpg";
	public static final String DEFAULT_BANNER_LANDING = "/events/10";

	public static final Long DEFAULT_SPLASH_ID = 99L;

	private MainPageResponseFixture() {}

	public static MainSectionItem createSectionItem() {
		return new MainSectionItem(
			DEFAULT_RESTAURANT_ID,
			DEFAULT_RESTAURANT_NAME,
			DEFAULT_DISTANCE,
			DEFAULT_CATEGORIES,
			DEFAULT_THUMBNAIL,
			DEFAULT_SUMMARY);
	}

	public static MainSectionItem createSectionItem(Long restaurantId, String name, String summary) {
		return new MainSectionItem(
			restaurantId,
			name,
			DEFAULT_DISTANCE,
			DEFAULT_CATEGORIES,
			DEFAULT_THUMBNAIL,
			summary);
	}

	public static Banners createEmptyBanners() {
		return new Banners(false, List.of());
	}

	public static Banners createBanners() {
		return new Banners(true,
			List.of(new BannerItem(DEFAULT_BANNER_ID, DEFAULT_BANNER_IMAGE, DEFAULT_BANNER_LANDING, 1)));
	}

	public static SplashPromotionResponse createSplashPromotion() {
		return new SplashPromotionResponse(
			DEFAULT_SPLASH_ID,
			"스플래시 제목",
			"스플래시 내용",
			"https://example.com/splash-thumb.jpg",
			Instant.parse("2026-03-01T00:00:00Z"),
			Instant.parse("2026-03-31T23:59:59Z"),
			List.of("https://example.com/splash-detail.jpg"));
	}

	public static MainPageResponse createMainPageResponse() {
		MainSectionItem item = createSectionItem();
		return new MainPageResponse(
			createEmptyBanners(),
			List.of(
				new Section("SPONSORED", "Sponsored", List.of()),
				new Section("HOT", "이번주 Hot", List.of(item)),
				new Section("NEW", "신규 개장", List.of(item)),
				new Section("AI_RECOMMEND", "AI 추천", List.of(item))),
			null);
	}

	public static HomePageResponse createHomePageResponse() {
		MainSectionItem item = new MainSectionItem(
			DEFAULT_RESTAURANT_ID, DEFAULT_RESTAURANT_NAME, 120.0,
			DEFAULT_CATEGORIES, DEFAULT_THUMBNAIL, "요약");
		return new HomePageResponse(
			createBanners(),
			List.of(
				new HomePageResponse.Section("NEW", "신규 개장", List.of(item)),
				new HomePageResponse.Section("HOT", "이번주 Hot", List.of(item))),
			createSplashPromotion());
	}

	public static AiRecommendResponse createAiRecommendResponse() {
		MainSectionItem item = new MainSectionItem(
			2L, "카페", 80.0, List.of("카페", "디저트"), "https://example.com/img2.jpg", "AI 요약");
		return new AiRecommendResponse(
			new AiRecommendResponse.Section("AI_RECOMMEND", "AI 추천", List.of(item)));
	}
}
