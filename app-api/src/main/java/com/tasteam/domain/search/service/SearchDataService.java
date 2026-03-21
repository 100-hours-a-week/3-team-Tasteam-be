package com.tasteam.domain.search.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchGroupRepository;
import com.tasteam.domain.search.repository.SearchQueryRepository;
import com.tasteam.domain.search.repository.SearchRestaurantRepository;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchDataService {

	private final SearchGroupRepository searchGroupRepository;
	private final SearchQueryRepository searchQueryRepository;
	private final SearchRestaurantRepository searchRestaurantRepository;
	private final CursorCodec cursorCodec;

	public record GroupData(List<Group> groups, List<Long> groupIds) {
	}

	public record RestaurantPageData(
		CursorPageBuilder.Page<SearchRestaurantCursorRow> page,
		List<Long> restaurantIds,
		Map<Long, List<String>> categories) {
	}

	@Transactional(readOnly = true)
	public GroupData fetchGroups(String keyword, int pageSize) {
		List<Group> groups = searchGroupRepository.searchByKeyword(keyword, GroupStatus.ACTIVE, pageSize);
		List<Long> groupIds = groups.stream().map(Group::getId).toList();
		return new GroupData(groups, groupIds);
	}

	@Cacheable(cacheNames = "search-restaurant-first-page", key = "T(String).format('%s_%.2f_%.2f_%.0f', #keyword, #latitude != null ? #latitude : 0.0, #longitude != null ? #longitude : 0.0, #radiusMeters != null ? #radiusMeters : 0.0)", condition = "#cursorToken == null")
	@Transactional(readOnly = true)
	public RestaurantPageData fetchRestaurants(String keyword, String cursorToken, int pageSize,
		Double latitude, Double longitude, Double radiusMeters) {
		return fetchRestaurantsInternal(keyword, cursorToken, pageSize, latitude, longitude, radiusMeters, false);
	}

	@Transactional(readOnly = true)
	public RestaurantPageData fetchRestaurantsWithFallback(String keyword, String cursorToken, int pageSize,
		Double latitude, Double longitude, Double radiusMeters) {
		return fetchRestaurantsInternal(keyword, cursorToken, pageSize, latitude, longitude, radiusMeters, true);
	}

	private RestaurantPageData fetchRestaurantsInternal(String keyword, String cursorToken, int pageSize,
		Double latitude, Double longitude, Double radiusMeters, boolean useFallback) {
		CursorPageBuilder<SearchCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursorToken,
			SearchCursor.class);
		if (pageBuilder.isInvalid()) {
			return new RestaurantPageData(CursorPageBuilder.Page.empty(), List.of(), Map.of());
		}

		List<SearchRestaurantCursorRow> result = useFallback
			? searchQueryRepository.searchRestaurantsByKeywordWithFallback(
				keyword, pageBuilder.cursor(), CursorPageBuilder.fetchSize(pageSize),
				latitude, longitude, radiusMeters)
			: searchQueryRepository.searchRestaurantsByKeyword(
				keyword, pageBuilder.cursor(), CursorPageBuilder.fetchSize(pageSize),
				latitude, longitude, radiusMeters);

		CursorPageBuilder.Page<SearchRestaurantCursorRow> page = pageBuilder.build(
			result,
			pageSize,
			last -> new SearchCursor(
				last.nameExact(),
				last.nameSimilarity(),
				last.ftsRank(),
				last.distanceMeters(),
				last.categoryMatch(),
				last.addressMatch(),
				last.restaurant().updatedAt(),
				last.restaurant().id()));

		List<Long> restaurantIds = page.items().stream()
			.map(row -> row.restaurant().id())
			.toList();

		Map<Long, List<String>> categories = restaurantIds.isEmpty()
			? Map.of()
			: searchRestaurantRepository.findCategoriesByRestaurantIds(restaurantIds).stream()
				.collect(Collectors.groupingBy(
					RestaurantCategoryProjection::getRestaurantId,
					Collectors.mapping(RestaurantCategoryProjection::getCategoryName, Collectors.toList())));

		return new RestaurantPageData(page, restaurantIds, categories);
	}
}
