package com.tasteam.domain.main.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

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
import com.tasteam.domain.restaurant.service.FoodCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainService {

	private static final long MAIN_QUERY_TIMEOUT_SECONDS = 3L;
	private static final int HOT_GROUP_ITEM_LIMIT = 1;
	private static final int DISTANCE_GROUP_ITEM_LIMIT = 10;

	private final MainDataService mainDataService;
	private final MainRecommendationService mainRecommendationService;
	private final MainMetadataLoader metadataLoader;
	private final MainGroupRepository groupRepository;
	private final FoodCategoryService foodCategoryService;
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

		List<MainRestaurantDistanceProjection> hotRestaurants = getWithTimeout(hotFuture,
			() -> mainDataService.fetchHotSectionAll());
		List<MainRestaurantDistanceProjection> newRestaurants = getWithTimeout(newFuture,
			() -> mainDataService.fetchNewSectionAll());
		List<MainRestaurantDistanceProjection> aiRestaurants = getWithTimeout(aiFuture,
			() -> mainDataService.fetchAiSectionAll());

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
		List<String> categoryNames = loadHomeCategoryNames();

		CompletableFuture<List<MainRestaurantDistanceProjection>> recommendFuture = CompletableFuture.supplyAsync(
			() -> fetchRecommendHomeSection(memberId, location), mainQueryExecutor);
		Map<String, CompletableFuture<List<MainRestaurantDistanceProjection>>> hotGroupFutures = new LinkedHashMap<>();
		Map<String, CompletableFuture<List<MainRestaurantDistanceProjection>>> distanceGroupFutures = new LinkedHashMap<>();

		for (String categoryName : categoryNames) {
			hotGroupFutures.put(categoryName, CompletableFuture.supplyAsync(
				() -> fetchHotHomeGroup(location, categoryName), mainQueryExecutor));
			distanceGroupFutures.put(categoryName, CompletableFuture.supplyAsync(
				() -> fetchDistanceHomeGroup(location, categoryName), mainQueryExecutor));
		}

		List<MainRestaurantDistanceProjection> recommendRestaurants = getWithTimeout(recommendFuture, List::of);
		Map<String, List<MainRestaurantDistanceProjection>> hotGroups = new LinkedHashMap<>();
		Map<String, List<MainRestaurantDistanceProjection>> distanceGroups = new LinkedHashMap<>();

		for (String categoryName : categoryNames) {
			hotGroups.put(categoryName,
				getWithTimeout(hotGroupFutures.get(categoryName), () -> fallbackHotHomeGroup(location, categoryName)));
			distanceGroups.put(categoryName,
				getWithTimeout(distanceGroupFutures.get(categoryName), List::of));
		}

		List<Long> allIds = new ArrayList<>(collectAllIds(recommendRestaurants));
		hotGroups.values().forEach(restaurants -> restaurants.forEach(r -> allIds.add(r.getId())));
		distanceGroups.values().forEach(restaurants -> restaurants.forEach(r -> allIds.add(r.getId())));
		SectionMetadata metadata = fetchMetadata(new ArrayList<>(new HashSet<>(allIds)));

		List<HomePageResponse.Section> sections = List.of(
			HomePageResponse.Section.items("RECOMMEND", "당신을 위한 추천", toSectionItems(recommendRestaurants, metadata)),
			HomePageResponse.Section.groups("HOT", "인기 음식점", toHomeGroups(hotGroups, metadata, false)),
			HomePageResponse.Section.groups("DISTANCE", "가까운 음식점", toHomeGroups(distanceGroups, metadata, true)));

		Banners banners = fetchBanners();
		SplashPromotionResponse splashPromotion = promotionService.getSplashPromotion().orElse(null);

		return new HomePageResponse(banners, sections, splashPromotion);
	}

	public AiRecommendResponse getAiRecommend(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		List<MainRestaurantDistanceProjection> aiRestaurants = getWithTimeout(
			CompletableFuture.supplyAsync(() -> fetchAiSection(location), mainQueryExecutor),
			() -> mainDataService.fetchAiSectionAll());

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

	private List<MainRestaurantDistanceProjection> fetchRecommendHomeSection(Long memberId, LocationContext location) {
		Double latitude = location.hasLocation() ? location.latitude() : null;
		Double longitude = location.hasLocation() ? location.longitude() : null;
		return mainRecommendationService.fetchRecommendSection(memberId, latitude, longitude);
	}

	private List<String> loadHomeCategoryNames() {
		return foodCategoryService.getFoodCategories().stream()
			.map(category -> category.name())
			.toList();
	}

	private List<MainRestaurantDistanceProjection> fetchHotHomeGroup(LocationContext location, String categoryName) {
		if (location.hasLocation()) {
			return mainDataService.fetchHotCategorySectionByLocation(
				location.latitude(),
				location.longitude(),
				categoryName,
				HOT_GROUP_ITEM_LIMIT);
		}
		return mainDataService.fetchHotCategorySectionAll(categoryName, HOT_GROUP_ITEM_LIMIT);
	}

	private List<MainRestaurantDistanceProjection> fallbackHotHomeGroup(
		LocationContext location,
		String categoryName) {
		if (location.hasLocation()) {
			return mainDataService.fetchHotCategorySectionByLocation(
				location.latitude(),
				location.longitude(),
				categoryName,
				HOT_GROUP_ITEM_LIMIT);
		}
		return mainDataService.fetchHotCategorySectionAll(categoryName, HOT_GROUP_ITEM_LIMIT);
	}

	private List<MainRestaurantDistanceProjection> fetchDistanceHomeGroup(
		LocationContext location,
		String categoryName) {
		if (!location.hasLocation()) {
			return List.of();
		}
		return mainDataService.fetchDistanceCategorySectionByLocation(
			location.latitude(),
			location.longitude(),
			categoryName,
			DISTANCE_GROUP_ITEM_LIMIT);
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

		Map<Long, List<String>> categories = getWithTimeout(catFuture, Map::of);
		Map<Long, String> thumbnails = getWithTimeout(thumbFuture, Map::of);
		Map<Long, String> summaries = getWithTimeout(summaryFuture, Map::of);
		return new SectionMetadata(categories, thumbnails, summaries);
	}

	private <T> T getWithTimeout(CompletableFuture<T> future, Supplier<T> fallback) {
		try {
			return future.get(MAIN_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return fallback.get();
		} catch (TimeoutException e) {
			future.cancel(true);
			return fallback.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException re) {
				throw re;
			}
			return fallback.get();
		}
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

	private List<HomePageResponse.Group> toHomeGroups(
		Map<String, List<MainRestaurantDistanceProjection>> groupedRestaurants,
		SectionMetadata metadata,
		boolean distanceSection) {
		return groupedRestaurants.entrySet().stream()
			.filter(entry -> !entry.getValue().isEmpty())
			.map(entry -> {
				List<MainSectionItem> items = toSectionItems(entry.getValue(), metadata);
				if (distanceSection) {
					items = items.stream()
						.sorted(java.util.Comparator.comparing(
							MainSectionItem::distanceMeter,
							java.util.Comparator.nullsLast(Double::compareTo)))
						.toList();
				}
				return new HomePageResponse.Group(
					entry.getKey(),
					entry.getKey(),
					items);
			})
			.filter(group -> !group.items().isEmpty())
			.toList();
	}

	private record SectionMetadata(
		Map<Long, List<String>> categories,
		Map<Long, String> thumbnails,
		Map<Long, String> summaries) {
	}
}
