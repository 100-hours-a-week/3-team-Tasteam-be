package com.tasteam.domain.main.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.main.dto.response.HomePageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse.Banners;
import com.tasteam.domain.main.dto.response.MainPageResponse.Section;
import com.tasteam.domain.main.dto.response.MainSectionItem;
import com.tasteam.domain.main.repository.MainGroupRepository;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainService {

	private final MainDataService mainDataService;
	private final MainMetadataLoader metadataLoader;
	private final MainGroupRepository groupRepository;
	private final PromotionService promotionService;
	@Qualifier("mainQueryExecutor")
	private final Executor mainQueryExecutor;

	public MainPageResponse getMain(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		CompletableFuture<List<MainRestaurantDistanceProjection>> hotFuture = CompletableFuture.supplyAsync(
			() -> fetchHotSection(location), mainQueryExecutor);
		CompletableFuture<List<MainRestaurantDistanceProjection>> newFuture = CompletableFuture.supplyAsync(
			() -> fetchNewSection(location), mainQueryExecutor);
		CompletableFuture<List<MainRestaurantDistanceProjection>> aiFuture = CompletableFuture.supplyAsync(
			() -> fetchAiSection(location), mainQueryExecutor);

		CompletableFuture.allOf(hotFuture, newFuture, aiFuture).join();

		List<MainRestaurantDistanceProjection> hotRestaurants = hotFuture.join();
		List<MainRestaurantDistanceProjection> newRestaurants = newFuture.join();
		List<MainRestaurantDistanceProjection> aiRestaurants = aiFuture.join();

		SectionMetadata metadata = fetchMetadata(collectAllIds(hotRestaurants, newRestaurants, aiRestaurants));

		List<Section> sections = List.of(
			new Section("SPONSORED", "Sponsored", List.of()),
			new Section("HOT", "이번주 Hot", toSectionItems(hotRestaurants, metadata)),
			new Section("NEW", "신규 개장", toSectionItems(newRestaurants, metadata)),
			new Section("AI_RECOMMEND", "AI 추천", toSectionItems(aiRestaurants, metadata)));

		Banners banners = fetchBanners();
		SplashPromotionResponse splashPromotion = promotionService.getSplashPromotion().orElse(null);

		return new MainPageResponse(banners, sections, splashPromotion);
	}

	public HomePageResponse getHome(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		CompletableFuture<List<MainRestaurantDistanceProjection>> newFuture = CompletableFuture.supplyAsync(
			() -> fetchNewSection(location), mainQueryExecutor);
		CompletableFuture<List<MainRestaurantDistanceProjection>> hotFuture = CompletableFuture.supplyAsync(
			() -> fetchHotSection(location), mainQueryExecutor);

		CompletableFuture.allOf(newFuture, hotFuture).join();

		List<MainRestaurantDistanceProjection> newRestaurants = newFuture.join();
		List<MainRestaurantDistanceProjection> hotRestaurants = hotFuture.join();

		SectionMetadata metadata = fetchMetadata(collectAllIds(newRestaurants, hotRestaurants));

		List<HomePageResponse.Section> sections = List.of(
			new HomePageResponse.Section("NEW", "신규 개장", toSectionItems(newRestaurants, metadata)),
			new HomePageResponse.Section("HOT", "이번주 Hot", toSectionItems(hotRestaurants, metadata)));

		Banners banners = fetchBanners();
		SplashPromotionResponse splashPromotion = promotionService.getSplashPromotion().orElse(null);

		return new HomePageResponse(banners, sections, splashPromotion);
	}

	public AiRecommendResponse getAiRecommend(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		List<MainRestaurantDistanceProjection> aiRestaurants = CompletableFuture
			.supplyAsync(() -> fetchAiSection(location), mainQueryExecutor)
			.join();

		SectionMetadata metadata = fetchMetadata(collectAllIds(aiRestaurants));

		AiRecommendResponse.Section section = new AiRecommendResponse.Section(
			"AI_RECOMMEND", "AI 추천", toSectionItems(aiRestaurants, metadata));

		return new AiRecommendResponse(section);
	}

	private Banners fetchBanners() {
		var bannerPromotions = promotionService.getBannerPromotions(
			org.springframework.data.domain.PageRequest.of(0, 10));

		if (bannerPromotions.items().isEmpty()) {
			return new Banners(false, List.of());
		}

		List<MainPageResponse.BannerItem> bannerItems = bannerPromotions.items().stream()
			.map(promotion -> new MainPageResponse.BannerItem(
				promotion.id(),
				promotion.bannerImageUrl(),
				promotion.landingUrl(),
				null))
			.toList();

		return new Banners(true, bannerItems);
	}

	private LocationContext resolveLocation(Long memberId, MainPageRequest request) {
		if (request.hasLocation()) {
			return new LocationContext(request.latitude(), request.longitude(), true);
		}

		if (memberId != null) {
			return groupRepository.findFirstGroupLocationByMemberId(memberId)
				.map(loc -> new LocationContext(loc.getLatitude(), loc.getLongitude(), true))
				.orElse(new LocationContext(null, null, false));
		}

		return new LocationContext(null, null, false);
	}

	private List<MainRestaurantDistanceProjection> fetchHotSection(LocationContext location) {
		if (location.hasLocation()) {
			return mainDataService.fetchHotSectionByLocation(location.latitude(), location.longitude());
		}
		return mainDataService.fetchHotSectionAll();
	}

	private List<MainRestaurantDistanceProjection> fetchNewSection(LocationContext location) {
		if (location.hasLocation()) {
			return mainDataService.fetchNewSectionByLocation(location.latitude(), location.longitude());
		}
		return mainDataService.fetchNewSectionAll();
	}

	private List<MainRestaurantDistanceProjection> fetchAiSection(LocationContext location) {
		if (location.hasLocation()) {
			return mainDataService.fetchAiSectionByLocation(location.latitude(), location.longitude());
		}
		return mainDataService.fetchAiSectionAll();
	}

	@SafeVarargs
	private List<Long> collectAllIds(List<MainRestaurantDistanceProjection>... lists) {
		Set<Long> idSet = new HashSet<>();
		for (List<MainRestaurantDistanceProjection> list : lists) {
			list.forEach(r -> idSet.add(r.getId()));
		}
		return new ArrayList<>(idSet);
	}

	private SectionMetadata fetchMetadata(List<Long> allIds) {
		CompletableFuture<Map<Long, List<String>>> catFuture = CompletableFuture.supplyAsync(
			() -> metadataLoader.loadCategories(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> thumbFuture = CompletableFuture.supplyAsync(
			() -> metadataLoader.loadThumbnails(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> summaryFuture = CompletableFuture.supplyAsync(
			() -> metadataLoader.loadSummaries(allIds), mainQueryExecutor);

		CompletableFuture.allOf(catFuture, thumbFuture, summaryFuture).join();
		return new SectionMetadata(catFuture.join(), thumbFuture.join(), summaryFuture.join());
	}

	private List<MainSectionItem> toSectionItems(
		List<MainRestaurantDistanceProjection> restaurants, SectionMetadata metadata) {
		return restaurants.stream()
			.map(r -> new MainSectionItem(
				r.getId(),
				r.getName(),
				r.getDistanceMeter(),
				metadata.categories().getOrDefault(r.getId(), List.of()),
				metadata.thumbnails().get(r.getId()),
				metadata.summaries().get(r.getId())))
			.toList();
	}

	private record SectionMetadata(
		Map<Long, List<String>> categories,
		Map<Long, String> thumbnails,
		Map<Long, String> summaries) {
	}
}
