package com.tasteam.domain.main.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.main.dto.response.HomePageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse.Banners;
import com.tasteam.domain.main.dto.response.MainPageResponse.Section;
import com.tasteam.domain.main.dto.response.MainPageResponse.SectionItem;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryRow;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.policy.RestaurantSearchPolicy;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainService {

	private static final List<Long> SENTINEL_EXCLUDE = List.of(-1L);

	private final RestaurantRepository restaurantRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final RestaurantReviewSummaryRepository restaurantReviewSummaryRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final GroupRepository groupRepository;
	private final FileService fileService;
	private final PromotionService promotionService;

	@Transactional(readOnly = true)
	public MainPageResponse getMain(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		List<MainRestaurantDistanceProjection> hotRestaurants = fetchHotSection(location);
		List<MainRestaurantDistanceProjection> newRestaurants = fetchNewSection(location);
		List<MainRestaurantDistanceProjection> aiRestaurants = fetchAiRecommendSection(location);

		Set<Long> allIdSet = new HashSet<>();
		Stream.of(hotRestaurants, newRestaurants, aiRestaurants)
			.flatMap(List::stream)
			.forEach(r -> allIdSet.add(r.getId()));
		List<Long> allIds = new ArrayList<>(allIdSet);

		Map<Long, String> categoryByRestaurant = fetchCategories(allIds);
		Map<Long, String> thumbnailByRestaurant = fetchThumbnails(allIds);
		Map<Long, String> summaryByRestaurant = fetchSummaries(allIds);

		List<Section> sections = List.of(
			new Section("SPONSORED", "Sponsored", List.of()),
			new Section("HOT", "이번주 Hot",
				toSectionItems(hotRestaurants, categoryByRestaurant, thumbnailByRestaurant, summaryByRestaurant)),
			new Section("NEW", "신규 개장",
				toSectionItems(newRestaurants, categoryByRestaurant, thumbnailByRestaurant, summaryByRestaurant)),
			new Section("AI_RECOMMEND", "AI 추천",
				toSectionItems(aiRestaurants, categoryByRestaurant, thumbnailByRestaurant, summaryByRestaurant)));

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

	@Transactional(readOnly = true)
	public HomePageResponse getHome(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		List<MainRestaurantDistanceProjection> newRestaurants = fetchNewSection(location);
		List<MainRestaurantDistanceProjection> hotRestaurants = fetchHotSection(location);

		Set<Long> allIdSet = new HashSet<>();
		Stream.of(newRestaurants, hotRestaurants)
			.flatMap(List::stream)
			.forEach(r -> allIdSet.add(r.getId()));
		List<Long> allIds = new ArrayList<>(allIdSet);

		Map<Long, String> categoryByRestaurant = fetchCategories(allIds);
		Map<Long, String> thumbnailByRestaurant = fetchThumbnails(allIds);
		Map<Long, String> summaryByRestaurant = fetchSummaries(allIds);

		List<HomePageResponse.Section> sections = List.of(
			new HomePageResponse.Section("NEW", "신규 개장",
				toHomeSectionItems(newRestaurants, categoryByRestaurant, thumbnailByRestaurant, summaryByRestaurant)),
			new HomePageResponse.Section("HOT", "이번주 Hot",
				toHomeSectionItems(hotRestaurants, categoryByRestaurant, thumbnailByRestaurant, summaryByRestaurant)));

		return new HomePageResponse(sections);
	}

	@Transactional(readOnly = true)
	public AiRecommendResponse getAiRecommend(Long memberId, MainPageRequest request) {
		LocationContext location = resolveLocation(memberId, request);

		List<MainRestaurantDistanceProjection> aiRestaurants = fetchAiRecommendSection(location);

		List<Long> allIds = aiRestaurants.stream()
			.map(MainRestaurantDistanceProjection::getId)
			.toList();

		Map<Long, String> categoryByRestaurant = fetchCategories(allIds);
		Map<Long, String> thumbnailByRestaurant = fetchThumbnails(allIds);
		Map<Long, String> summaryByRestaurant = fetchSummaries(allIds);

		AiRecommendResponse.Section section = new AiRecommendResponse.Section(
			"AI_RECOMMEND",
			"AI 추천",
			toAiSectionItems(aiRestaurants, categoryByRestaurant, thumbnailByRestaurant, summaryByRestaurant));

		return new AiRecommendResponse(section);
	}

	private LocationContext resolveLocation(Long memberId, MainPageRequest request) {
		if (request.hasLocation()) {
			return new LocationContext(request.latitude(), request.longitude(), true);
		}

		if (memberId != null) {
			List<MemberGroupSummaryRow> groups = groupMemberRepository.findMemberGroupSummaries(
				memberId, GroupStatus.ACTIVE);

			for (MemberGroupSummaryRow groupSummary : groups) {
				Group group = groupRepository.findByIdAndDeletedAtIsNull(groupSummary.groupId())
					.orElse(null);
				if (group != null && group.getLocation() != null) {
					return new LocationContext(
						group.getLocation().getY(),
						group.getLocation().getX(),
						true);
				}
			}
		}

		return new LocationContext(null, null, false);
	}

	private List<MainRestaurantDistanceProjection> fetchHotSection(LocationContext location) {
		if (location.hasLocation()) {
			return fetchWithRadiusExpansion(location, this::queryHotWithLocation);
		}
		return fetchWithoutLocation(this::queryHotAll);
	}

	private List<MainRestaurantDistanceProjection> fetchNewSection(LocationContext location) {
		if (location.hasLocation()) {
			return fetchWithRadiusExpansion(location, this::queryNewWithLocation);
		}
		return fetchWithoutLocation(this::queryNewAll);
	}

	private List<MainRestaurantDistanceProjection> fetchAiRecommendSection(LocationContext location) {
		if (location.hasLocation()) {
			return fetchWithRadiusExpansion(location, this::queryAiRecommendWithLocation);
		}
		return fetchWithoutLocation(this::queryAiRecommendAll);
	}

	private List<MainRestaurantDistanceProjection> fetchWithRadiusExpansion(
		LocationContext location, LocationQuery query) {
		LinkedHashMap<Long, MainRestaurantDistanceProjection> collected = new LinkedHashMap<>();

		for (int radius : RestaurantSearchPolicy.EXPANDED_RADII) {
			List<MainRestaurantDistanceProjection> results = query.execute(
				location.latitude(), location.longitude(), radius, RestaurantSearchPolicy.SECTION_SIZE);

			for (MainRestaurantDistanceProjection r : results) {
				collected.putIfAbsent(r.getId(), r);
				if (collected.size() >= RestaurantSearchPolicy.SECTION_SIZE) {
					return new ArrayList<>(collected.values());
				}
			}
		}

		if (collected.size() < RestaurantSearchPolicy.SECTION_SIZE) {
			fillWithRandom(collected, RestaurantSearchPolicy.SECTION_SIZE - collected.size());
		}

		return new ArrayList<>(collected.values());
	}

	private List<MainRestaurantDistanceProjection> fetchWithoutLocation(NoLocationQuery query) {
		List<Long> excludeIds = SENTINEL_EXCLUDE;
		List<MainRestaurantDistanceProjection> results = query.execute(excludeIds, RestaurantSearchPolicy.SECTION_SIZE);

		if (results.size() >= RestaurantSearchPolicy.SECTION_SIZE) {
			return results;
		}

		LinkedHashMap<Long, MainRestaurantDistanceProjection> collected = new LinkedHashMap<>();
		for (MainRestaurantDistanceProjection r : results) {
			collected.put(r.getId(), r);
		}

		fillWithRandom(collected, RestaurantSearchPolicy.SECTION_SIZE - collected.size());
		return new ArrayList<>(collected.values());
	}

	private void fillWithRandom(LinkedHashMap<Long, MainRestaurantDistanceProjection> collected, int needed) {
		if (needed <= 0) {
			return;
		}
		List<Long> excludeIds = collected.isEmpty() ? SENTINEL_EXCLUDE : new ArrayList<>(collected.keySet());
		List<MainRestaurantDistanceProjection> fillers = restaurantRepository.findRandomRestaurants(
			excludeIds, needed);
		for (MainRestaurantDistanceProjection r : fillers) {
			collected.putIfAbsent(r.getId(), r);
		}
	}

	private List<MainRestaurantDistanceProjection> queryHotWithLocation(
		double lat, double lon, int radius, int limit) {
		return restaurantRepository.findHotRestaurants(lat, lon, radius, limit);
	}

	private List<MainRestaurantDistanceProjection> queryNewWithLocation(
		double lat, double lon, int radius, int limit) {
		return restaurantRepository.findNewRestaurants(lat, lon, radius, limit);
	}

	private List<MainRestaurantDistanceProjection> queryAiRecommendWithLocation(
		double lat, double lon, int radius, int limit) {
		return restaurantRepository.findAiRecommendRestaurants(lat, lon, radius, limit);
	}

	private List<MainRestaurantDistanceProjection> queryHotAll(List<Long> excludeIds, int limit) {
		return restaurantRepository.findHotRestaurantsAll(excludeIds, limit);
	}

	private List<MainRestaurantDistanceProjection> queryNewAll(List<Long> excludeIds, int limit) {
		return restaurantRepository.findNewRestaurantsAll(excludeIds, limit);
	}

	private List<MainRestaurantDistanceProjection> queryAiRecommendAll(List<Long> excludeIds, int limit) {
		return restaurantRepository.findAiRecommendRestaurantsAll(excludeIds, limit);
	}

	private Map<Long, String> fetchCategories(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		return restaurantFoodCategoryRepository
			.findCategoriesByRestaurantIds(restaurantIds)
			.stream()
			.collect(groupingBy(
				RestaurantCategoryProjection::getRestaurantId,
				Collectors.mapping(
					RestaurantCategoryProjection::getCategoryName,
					Collectors.collectingAndThen(Collectors.toList(),
						list -> list.isEmpty() ? null : list.getFirst()))));
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
			.collect(Collectors.toMap(
				RestaurantReviewSummary::getRestaurantId,
				s -> toSummaryString(s.getSummaryJson())));
	}

	private static String toSummaryString(Map<String, Object> summaryJson) {
		if (summaryJson == null) {
			return null;
		}
		Object overall = summaryJson.get("overall_summary");
		return overall != null ? overall.toString() : null;
	}

	private List<SectionItem> toSectionItems(
		List<MainRestaurantDistanceProjection> restaurants,
		Map<Long, String> categoryByRestaurant,
		Map<Long, String> thumbnailByRestaurant,
		Map<Long, String> summaryByRestaurant) {
		return restaurants.stream()
			.map(restaurant -> new SectionItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getDistanceMeter(),
				categoryByRestaurant.get(restaurant.getId()),
				thumbnailByRestaurant.get(restaurant.getId()),
				false,
				summaryByRestaurant.get(restaurant.getId())))
			.toList();
	}

	private List<HomePageResponse.SectionItem> toHomeSectionItems(
		List<MainRestaurantDistanceProjection> restaurants,
		Map<Long, String> categoryByRestaurant,
		Map<Long, String> thumbnailByRestaurant,
		Map<Long, String> summaryByRestaurant) {
		return restaurants.stream()
			.map(restaurant -> new HomePageResponse.SectionItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getDistanceMeter(),
				categoryByRestaurant.get(restaurant.getId()),
				thumbnailByRestaurant.get(restaurant.getId()),
				summaryByRestaurant.get(restaurant.getId())))
			.toList();
	}

	private List<AiRecommendResponse.SectionItem> toAiSectionItems(
		List<MainRestaurantDistanceProjection> restaurants,
		Map<Long, String> categoryByRestaurant,
		Map<Long, String> thumbnailByRestaurant,
		Map<Long, String> summaryByRestaurant) {
		return restaurants.stream()
			.map(restaurant -> new AiRecommendResponse.SectionItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getDistanceMeter(),
				categoryByRestaurant.get(restaurant.getId()),
				thumbnailByRestaurant.get(restaurant.getId()),
				summaryByRestaurant.get(restaurant.getId())))
			.toList();
	}

	private record LocationContext(Double latitude, Double longitude, boolean hasLocation) {
	}

	@FunctionalInterface
	private interface LocationQuery {
		List<MainRestaurantDistanceProjection> execute(double lat, double lon, int radius, int limit);
	}

	@FunctionalInterface
	private interface NoLocationQuery {
		List<MainRestaurantDistanceProjection> execute(List<Long> excludeIds, int limit);
	}
}
