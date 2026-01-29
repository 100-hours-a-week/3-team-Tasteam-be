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

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse.Banners;
import com.tasteam.domain.main.dto.response.MainPageResponse.Section;
import com.tasteam.domain.main.dto.response.MainPageResponse.SectionItem;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryRow;
import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.policy.RestaurantSearchPolicy;
import com.tasteam.domain.restaurant.repository.AiRestaurantReviewAnalysisRepository;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantImageRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantImageProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainService {

	private static final List<Long> SENTINEL_EXCLUDE = List.of(-1L);

	private final RestaurantRepository restaurantRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final RestaurantImageRepository restaurantImageRepository;
	private final AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final GroupRepository groupRepository;

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

		return new MainPageResponse(new Banners(false, List.of()), sections);
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
		return restaurantImageRepository
			.findRestaurantImages(restaurantIds)
			.stream()
			.collect(Collectors.toMap(
				RestaurantImageProjection::getRestaurantId,
				RestaurantImageProjection::getImageUrl,
				(existing, ignored) -> existing));
	}

	private Map<Long, String> fetchSummaries(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		return aiRestaurantReviewAnalysisRepository
			.findByRestaurantIdIn(restaurantIds)
			.stream()
			.collect(Collectors.toMap(
				AiRestaurantReviewAnalysis::getRestaurantId,
				AiRestaurantReviewAnalysis::getSummary));
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
