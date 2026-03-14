package com.tasteam.domain.main.service;

import static com.tasteam.domain.restaurant.service.RestaurantAiJsonParser.extractSummaryText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.main.dto.response.HomePageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse.Banners;
import com.tasteam.domain.main.dto.response.MainPageResponse.Section;
import com.tasteam.domain.main.dto.response.MainPageResponse.SectionItem;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainService {

	private final MainDataService mainDataService;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final RestaurantReviewSummaryRepository restaurantReviewSummaryRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final FileService fileService;
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

		Set<Long> allIdSet = new HashSet<>();
		Stream.of(hotRestaurants, newRestaurants, aiRestaurants)
			.flatMap(List::stream)
			.forEach(r -> allIdSet.add(r.getId()));
		List<Long> allIds = new ArrayList<>(allIdSet);

		CompletableFuture<Map<Long, List<String>>> catFuture = CompletableFuture.supplyAsync(
			() -> fetchCategories(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> thumbFuture = CompletableFuture.supplyAsync(
			() -> fetchThumbnails(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> summaryFuture = CompletableFuture.supplyAsync(
			() -> fetchSummaries(allIds), mainQueryExecutor);

		CompletableFuture.allOf(catFuture, thumbFuture, summaryFuture).join();

		Map<Long, List<String>> categoriesByRestaurant = catFuture.join();
		Map<Long, String> thumbnailByRestaurant = thumbFuture.join();
		Map<Long, String> summaryByRestaurant = summaryFuture.join();

		List<Section> sections = List.of(
			new Section("SPONSORED", "Sponsored", List.of()),
			new Section("HOT", "이번주 Hot",
				toSectionItems(hotRestaurants, categoriesByRestaurant, thumbnailByRestaurant, summaryByRestaurant)),
			new Section("NEW", "신규 개장",
				toSectionItems(newRestaurants, categoriesByRestaurant, thumbnailByRestaurant, summaryByRestaurant)),
			new Section("AI_RECOMMEND", "AI 추천",
				toSectionItems(aiRestaurants, categoriesByRestaurant, thumbnailByRestaurant, summaryByRestaurant)));

		Banners banners = fetchBanners();
		SplashPromotionResponse splashPromotion = promotionService.getSplashPromotion().orElse(null);

		return new MainPageResponse(banners, sections, splashPromotion);
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

	public HomePageResponse getHome(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		CompletableFuture<List<MainRestaurantDistanceProjection>> newFuture = CompletableFuture.supplyAsync(
			() -> fetchNewSection(location), mainQueryExecutor);
		CompletableFuture<List<MainRestaurantDistanceProjection>> hotFuture = CompletableFuture.supplyAsync(
			() -> fetchHotSection(location), mainQueryExecutor);

		CompletableFuture.allOf(newFuture, hotFuture).join();

		List<MainRestaurantDistanceProjection> newRestaurants = newFuture.join();
		List<MainRestaurantDistanceProjection> hotRestaurants = hotFuture.join();

		Set<Long> allIdSet = new HashSet<>();
		Stream.of(newRestaurants, hotRestaurants)
			.flatMap(List::stream)
			.forEach(r -> allIdSet.add(r.getId()));
		List<Long> allIds = new ArrayList<>(allIdSet);

		CompletableFuture<Map<Long, List<String>>> catFuture = CompletableFuture.supplyAsync(
			() -> fetchCategories(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> thumbFuture = CompletableFuture.supplyAsync(
			() -> fetchThumbnails(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> summaryFuture = CompletableFuture.supplyAsync(
			() -> fetchSummaries(allIds), mainQueryExecutor);

		CompletableFuture.allOf(catFuture, thumbFuture, summaryFuture).join();

		Map<Long, List<String>> categoriesByRestaurant = catFuture.join();
		Map<Long, String> thumbnailByRestaurant = thumbFuture.join();
		Map<Long, String> summaryByRestaurant = summaryFuture.join();

		List<HomePageResponse.Section> sections = List.of(
			new HomePageResponse.Section("NEW", "신규 개장",
				toHomeSectionItems(newRestaurants, categoriesByRestaurant, thumbnailByRestaurant, summaryByRestaurant)),
			new HomePageResponse.Section("HOT", "이번주 Hot",
				toHomeSectionItems(hotRestaurants, categoriesByRestaurant, thumbnailByRestaurant,
					summaryByRestaurant)));

		Banners mainBanners = fetchBanners();
		HomePageResponse.Banners banners = new HomePageResponse.Banners(
			mainBanners.enabled(),
			mainBanners.items().stream()
				.map(item -> new HomePageResponse.BannerItem(
					item.id(),
					item.imageUrl(),
					item.landingUrl(),
					item.order()))
				.toList());
		SplashPromotionResponse splashPromotion = promotionService.getSplashPromotion().orElse(null);

		return new HomePageResponse(banners, sections, splashPromotion);
	}

	public AiRecommendResponse getAiRecommend(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		List<MainRestaurantDistanceProjection> aiRestaurants = CompletableFuture
			.supplyAsync(() -> fetchAiSection(location), mainQueryExecutor)
			.join();

		List<Long> allIds = aiRestaurants.stream()
			.map(MainRestaurantDistanceProjection::getId)
			.toList();

		CompletableFuture<Map<Long, List<String>>> catFuture = CompletableFuture.supplyAsync(
			() -> fetchCategories(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> thumbFuture = CompletableFuture.supplyAsync(
			() -> fetchThumbnails(allIds), mainQueryExecutor);
		CompletableFuture<Map<Long, String>> summaryFuture = CompletableFuture.supplyAsync(
			() -> fetchSummaries(allIds), mainQueryExecutor);

		CompletableFuture.allOf(catFuture, thumbFuture, summaryFuture).join();

		AiRecommendResponse.Section section = new AiRecommendResponse.Section(
			"AI_RECOMMEND",
			"AI 추천",
			toAiSectionItems(aiRestaurants, catFuture.join(), thumbFuture.join(), summaryFuture.join()));

		return new AiRecommendResponse(section);
	}

	private LocationContext resolveLocation(Long memberId, MainPageRequest request) {
		if (request.hasLocation()) {
			return new LocationContext(request.latitude(), request.longitude(), true);
		}

		if (memberId != null) {
			return groupMemberRepository.findFirstGroupLocationByMemberId(memberId)
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

	private Map<Long, List<String>> fetchCategories(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		return restaurantFoodCategoryRepository
			.findCategoriesByRestaurantIds(restaurantIds)
			.stream()
			.collect(Collectors.groupingBy(
				RestaurantCategoryProjection::getRestaurantId,
				Collectors.mapping(
					RestaurantCategoryProjection::getCategoryName,
					Collectors.toList())));
	}

	private Map<Long, String> fetchThumbnails(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<DomainImageItem>> domainImages = fileService.getDomainImageUrls(
			DomainType.RESTAURANT,
			restaurantIds);
		return domainImages.entrySet().stream()
			.filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getFirst().url()));
	}

	private Map<Long, String> fetchSummaries(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		return restaurantReviewSummaryRepository
			.findByRestaurantIdIn(restaurantIds)
			.stream()
			.filter(summary -> extractSummaryText(summary.getSummaryJson()) != null)
			.collect(Collectors.toMap(
				RestaurantReviewSummary::getRestaurantId,
				s -> extractSummaryText(s.getSummaryJson()),
				(existing, replacement) -> existing));
	}

	private List<SectionItem> toSectionItems(
		List<MainRestaurantDistanceProjection> restaurants,
		Map<Long, List<String>> categoriesByRestaurant,
		Map<Long, String> thumbnailByRestaurant,
		Map<Long, String> summaryByRestaurant) {
		return restaurants.stream()
			.map(restaurant -> new SectionItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getDistanceMeter(),
				categoriesByRestaurant.getOrDefault(restaurant.getId(), List.of()),
				thumbnailByRestaurant.get(restaurant.getId()),
				false,
				summaryByRestaurant.get(restaurant.getId())))
			.toList();
	}

	private List<HomePageResponse.SectionItem> toHomeSectionItems(
		List<MainRestaurantDistanceProjection> restaurants,
		Map<Long, List<String>> categoriesByRestaurant,
		Map<Long, String> thumbnailByRestaurant,
		Map<Long, String> summaryByRestaurant) {
		return restaurants.stream()
			.map(restaurant -> new HomePageResponse.SectionItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getDistanceMeter(),
				categoriesByRestaurant.getOrDefault(restaurant.getId(), List.of()),
				thumbnailByRestaurant.get(restaurant.getId()),
				summaryByRestaurant.get(restaurant.getId())))
			.toList();
	}

	private List<AiRecommendResponse.SectionItem> toAiSectionItems(
		List<MainRestaurantDistanceProjection> restaurants,
		Map<Long, List<String>> categoriesByRestaurant,
		Map<Long, String> thumbnailByRestaurant,
		Map<Long, String> summaryByRestaurant) {
		return restaurants.stream()
			.map(restaurant -> new AiRecommendResponse.SectionItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getDistanceMeter(),
				categoriesByRestaurant.getOrDefault(restaurant.getId(), List.of()),
				thumbnailByRestaurant.get(restaurant.getId()),
				summaryByRestaurant.get(restaurant.getId())))
			.toList();
	}
}
