package com.tasteam.domain.search.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.entity.GroupStatus;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantImageRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantImageProjection;
import com.tasteam.domain.search.dto.RecentSearchCursor;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.request.RecentSearchQueryParams;
import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.RecentSearchListResponse;
import com.tasteam.domain.search.dto.response.SearchGroupSummary;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.domain.search.dto.response.SearchRestaurantItem;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.SearchErrorCode;
import com.tasteam.global.utils.CursorCodec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int THUMBNAIL_LIMIT = 3;

	private final GroupRepository groupRepository;
	private final RestaurantRepository restaurantRepository;
	private final RestaurantImageRepository restaurantImageRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final MemberSearchHistoryRepository memberSearchHistoryRepository;
	private final CursorCodec cursorCodec;

	@Transactional
	public SearchResponse search(Long memberId, SearchRequest request) {
		String keyword = request.keyword().trim();
		int pageSize = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
		SearchCursor cursor = cursorCodec.decodeOrNull(request.cursor(), SearchCursor.class);
		if (request.cursor() != null && cursor == null) {
			return emptyResponse();
		}

		recordSearchHistory(memberId, keyword);

		List<SearchGroupSummary> groups = searchGroups(keyword, pageSize);
		CursorPageResponse<SearchRestaurantItem> restaurants = searchRestaurants(keyword, cursor, pageSize);
		return new SearchResponse(
			new SearchResponse.SearchData(
				groups,
				restaurants,
				List.of()));
	}

	@Transactional(readOnly = true)
	public RecentSearchListResponse getRecentSearches(Long memberId, RecentSearchQueryParams params) {
		int pageSize = params.size() == null ? DEFAULT_PAGE_SIZE : params.size();
		RecentSearchCursor cursor = cursorCodec.decodeOrNull(params.cursor(), RecentSearchCursor.class);
		if (params.cursor() != null && cursor == null) {
			return new RecentSearchListResponse(List.of(), new RecentSearchListResponse.PageInfo(null, 0, false));
		}

		List<MemberSearchHistory> results = memberSearchHistoryRepository.findRecentSearches(
			memberId,
			cursor == null ? null : cursor.updatedAt(),
			cursor == null ? null : cursor.id(),
			PageRequest.of(0, pageSize + 1));

		boolean hasNext = results.size() > pageSize;
		List<MemberSearchHistory> pageContent = hasNext ? results.subList(0, pageSize) : results;

		String nextCursor = null;
		if (hasNext && !pageContent.isEmpty()) {
			MemberSearchHistory last = pageContent.getLast();
			nextCursor = cursorCodec.encode(new RecentSearchCursor(last.getUpdatedAt(), last.getId()));
		}

		List<RecentSearchListResponse.RecentSearchItem> data = pageContent.stream()
			.map(history -> new RecentSearchListResponse.RecentSearchItem(
				history.getId(),
				history.getKeyword(),
				history.getCount(),
				history.getUpdatedAt()))
			.toList();

		return new RecentSearchListResponse(
			data,
			new RecentSearchListResponse.PageInfo(nextCursor, data.size(), hasNext));
	}

	@Transactional
	public void deleteRecentSearch(Long memberId, Long historyId) {
		MemberSearchHistory history = memberSearchHistoryRepository
			.findByIdAndMemberIdAndDeletedAtIsNull(historyId, memberId)
			.orElseThrow(() -> new BusinessException(SearchErrorCode.RECENT_SEARCH_NOT_FOUND));
		history.delete();
	}

	private CursorPageResponse<SearchRestaurantItem> searchRestaurants(
		String keyword,
		SearchCursor cursor,
		int pageSize) {
		List<Restaurant> result = restaurantRepository.searchByKeyword(
			keyword,
			cursor == null ? null : cursor.updatedAt(),
			cursor == null ? null : cursor.id(),
			PageRequest.of(0, pageSize + 1));

		boolean hasNext = result.size() > pageSize;
		List<Restaurant> pageContent = hasNext ? result.subList(0, pageSize) : result;

		String nextCursor = null;
		if (hasNext && !pageContent.isEmpty()) {
			Restaurant last = pageContent.getLast();
			nextCursor = cursorCodec.encode(new SearchCursor(last.getUpdatedAt(), last.getId()));
		}

		List<Long> restaurantIds = pageContent.stream()
			.map(Restaurant::getId)
			.toList();

		Map<Long, List<RestaurantImageDto>> thumbnails = findRestaurantThumbnails(restaurantIds);
		Map<Long, List<String>> categories = findRestaurantCategories(restaurantIds);

		List<SearchRestaurantItem> items = pageContent.stream()
			.map(restaurant -> new SearchRestaurantItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getFullAddress(),
				categories.getOrDefault(restaurant.getId(), List.of()),
				thumbnails.getOrDefault(restaurant.getId(), List.of())))
			.toList();

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				nextCursor,
				hasNext,
				items.size()));
	}

	private List<SearchGroupSummary> searchGroups(String keyword, int pageSize) {
		List<Group> groups = groupRepository.searchByKeyword(
			keyword,
			GroupStatus.ACTIVE,
			PageRequest.of(0, pageSize));

		return groups.stream()
			.map(group -> new SearchGroupSummary(group.getId(), group.getName()))
			.toList();
	}

	private Map<Long, List<RestaurantImageDto>> findRestaurantThumbnails(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}

		return restaurantImageRepository
			.findRestaurantImages(restaurantIds)
			.stream()
			.collect(groupingBy(
				RestaurantImageProjection::getRestaurantId,
				Collectors.collectingAndThen(
					Collectors.mapping(
						projection -> new RestaurantImageDto(
							projection.getImageId(),
							projection.getImageUrl()),
						Collectors.toList()),
					list -> list.size() > THUMBNAIL_LIMIT ? list.subList(0, THUMBNAIL_LIMIT) : list)));
	}

	private Map<Long, List<String>> findRestaurantCategories(List<Long> restaurantIds) {
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
					Collectors.toList())));
	}

	private void recordSearchHistory(Long memberId, String keyword) {
		try {
			MemberSearchHistory history = memberSearchHistoryRepository
				.findByMemberIdAndKeywordAndDeletedAtIsNull(memberId, keyword)
				.orElseGet(() -> MemberSearchHistory.create(memberId, keyword));
			if (history.getId() == null) {
				memberSearchHistoryRepository.save(history);
			} else {
				history.incrementCount();
			}
		} catch (Exception ex) {
			log.warn("검색 히스토리 업데이트에 실패했습니다: {}", ex.getMessage());
		}
	}

	private SearchResponse emptyResponse() {
		return new SearchResponse(
			new SearchResponse.SearchData(
				List.of(),
				CursorPageResponse.empty(),
				List.of()));
	}
}
