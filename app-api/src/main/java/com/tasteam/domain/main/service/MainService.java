package com.tasteam.domain.main.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse.Banners;
import com.tasteam.domain.main.dto.response.MainPageResponse.Section;
import com.tasteam.domain.main.dto.response.MainPageResponse.SectionItem;
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

	private static final List<SectionDefinition> DEFAULT_SECTIONS = List.of(
		new SectionDefinition("SPONSORED", "Sponsored", true),
		new SectionDefinition("HOT", "이번주 Hot", false),
		new SectionDefinition("NEW", "신규 개장", false),
		new SectionDefinition("AI_RECOMMEND", "AI 추천", false));

	private final RestaurantRepository restaurantRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final RestaurantImageRepository restaurantImageRepository;
	private final AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;

	@Transactional(readOnly = true)
	public MainPageResponse getMain(Long memberId, MainPageRequest request) {
		List<MainRestaurantDistanceProjection> nearbyRestaurants = restaurantRepository.findNearbyRestaurants(
			request.latitude(),
			request.longitude(),
			RestaurantSearchPolicy.DEFAULT_RADIUS_METER,
			RestaurantSearchPolicy.DEFAULT_PAGE_SIZE);

		List<Long> restaurantIds = nearbyRestaurants.stream()
			.map(MainRestaurantDistanceProjection::getId)
			.toList();

		Map<Long, String> categoryByRestaurant = restaurantIds.isEmpty()
			? Map.of()
			: restaurantFoodCategoryRepository
				.findCategoriesByRestaurantIds(restaurantIds)
				.stream()
				.collect(groupingBy(
					RestaurantCategoryProjection::getRestaurantId,
					Collectors.mapping(
						RestaurantCategoryProjection::getCategoryName,
						Collectors.collectingAndThen(Collectors.toList(),
							list -> list.isEmpty() ? null : list.getFirst()))));

		Map<Long, String> thumbnailByRestaurant = restaurantIds.isEmpty()
			? Map.of()
			: restaurantImageRepository
				.findRestaurantImages(restaurantIds)
				.stream()
				.collect(Collectors.toMap(
					RestaurantImageProjection::getRestaurantId,
					RestaurantImageProjection::getImageUrl,
					(existing, ignored) -> existing));

		Map<Long, String> summaryByRestaurant = restaurantIds.isEmpty()
			? Map.of()
			: aiRestaurantReviewAnalysisRepository
				.findByRestaurantIdIn(restaurantIds)
				.stream()
				.collect(Collectors.toMap(
					analysis -> analysis.getRestaurant().getId(),
					analysis -> analysis.getSummary()));

		List<SectionItem> items = nearbyRestaurants.stream()
			.map(restaurant -> new SectionItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getDistanceMeter(),
				categoryByRestaurant.get(restaurant.getId()),
				thumbnailByRestaurant.get(restaurant.getId()),
				false,
				summaryByRestaurant.get(restaurant.getId())))
			.toList();

		List<Section> sections = DEFAULT_SECTIONS.stream()
			.map(section -> new Section(
				section.type(),
				section.title(),
				section.useItems() ? items : List.of()))
			.toList();

		return new MainPageResponse(
			new Banners(false, List.of()),
			sections);
	}

	private record SectionDefinition(String type, String title, boolean useItems) {
	}
}
