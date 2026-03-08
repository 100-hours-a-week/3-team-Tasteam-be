package com.tasteam.domain.search.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupQueryRepository;
import com.tasteam.domain.group.repository.projection.GroupMemberCountProjection;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryRepository;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchDataService {

	private final GroupQueryRepository groupQueryRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final SearchQueryRepository searchQueryRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final CursorCodec cursorCodec;

	public record GroupData(List<Group> groups, List<Long> groupIds, Map<Long, Long> memberCounts) {
	}

	public record RestaurantPageData(
		CursorPageBuilder.Page<SearchRestaurantCursorRow> page,
		List<Long> restaurantIds,
		Map<Long, List<String>> categories) {
	}

	@Transactional(readOnly = true)
	public GroupData fetchGroups(String keyword, int pageSize) {
		List<Group> groups = groupQueryRepository.searchByKeyword(keyword, GroupStatus.ACTIVE, pageSize);
		List<Long> groupIds = groups.stream().map(Group::getId).toList();
		Map<Long, Long> memberCounts = groupIds.isEmpty()
			? Map.of()
			: groupMemberRepository.findMemberCounts(groupIds).stream()
				.collect(Collectors.toMap(
					GroupMemberCountProjection::getGroupId,
					GroupMemberCountProjection::getMemberCount));
		return new GroupData(groups, groupIds, memberCounts);
	}

	@Transactional(readOnly = true)
	public RestaurantPageData fetchRestaurants(String keyword, String cursorToken, int pageSize,
		Double latitude, Double longitude, Double radiusMeters) {
		CursorPageBuilder<SearchCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursorToken,
			SearchCursor.class);
		if (pageBuilder.isInvalid()) {
			return new RestaurantPageData(CursorPageBuilder.Page.empty(), List.of(), Map.of());
		}

		List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
			keyword,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(pageSize),
			latitude,
			longitude,
			radiusMeters);

		CursorPageBuilder.Page<SearchRestaurantCursorRow> page = pageBuilder.build(
			result,
			pageSize,
			last -> new SearchCursor(
				last.nameExact(),
				last.nameSimilarity(),
				last.distanceMeters(),
				last.categoryMatch(),
				last.addressMatch(),
				last.restaurant().getUpdatedAt(),
				last.restaurant().getId()));

		List<Long> restaurantIds = page.items().stream()
			.map(SearchRestaurantCursorRow::restaurant)
			.map(Restaurant::getId)
			.toList();

		Map<Long, List<String>> categories = restaurantIds.isEmpty()
			? Map.of()
			: restaurantFoodCategoryRepository.findCategoriesByRestaurantIds(restaurantIds).stream()
				.collect(Collectors.groupingBy(
					RestaurantCategoryProjection::getRestaurantId,
					Collectors.mapping(RestaurantCategoryProjection::getCategoryName, Collectors.toList())));

		return new RestaurantPageData(page, restaurantIds, categories);
	}
}
