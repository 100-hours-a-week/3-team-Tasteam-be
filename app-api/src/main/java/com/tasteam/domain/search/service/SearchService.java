package com.tasteam.domain.search.service;

import static java.util.stream.Collectors.groupingBy;

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
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantImageRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantImageProjection;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.RecentSearchItem;
import com.tasteam.domain.search.dto.response.SearchGroupSummary;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.domain.search.dto.response.SearchRestaurantItem;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.repository.MemberSearchHistoryQueryRepository;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;
import com.tasteam.domain.search.repository.SearchQueryRepository;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
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

	private final GroupQueryRepository groupQueryRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final RestaurantImageRepository restaurantImageRepository;
	private final MemberSearchHistoryRepository memberSearchHistoryRepository;
	private final MemberSearchHistoryQueryRepository memberSearchHistoryQueryRepository;
	private final SearchQueryRepository searchQueryRepository;
	private final CursorCodec cursorCodec;
	private final SearchHistoryRecorder searchHistoryRecorder;

	@Transactional(readOnly = true)
	public SearchResponse search(Long memberId, SearchRequest request) {
		String keyword = request.keyword().trim();
		int pageSize = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
		SearchCursor cursor = cursorCodec.decodeOrNull(request.cursor(), SearchCursor.class);
		if (request.cursor() != null && !request.cursor().isBlank() && cursor == null) {
			return SearchResponse.emptyResponse();
		}

		List<SearchGroupSummary> groups = searchGroups(keyword, pageSize);
		CursorPageResponse<SearchRestaurantItem> restaurants = searchRestaurants(keyword, cursor, pageSize);

		if (!groups.isEmpty() || !restaurants.items().isEmpty()) {
			try {
				searchHistoryRecorder.recordSearchHistory(memberId, keyword);
			} catch (Exception ex) {
				log.warn("검색 히스토리 기록 중 예외 발생 (검색 결과에는 영향 없음): {}", ex.getMessage());
			}
		}

		return new SearchResponse(groups, restaurants);
	}

	@Transactional(readOnly = true)
	public OffsetPageResponse<RecentSearchItem> getRecentSearches(Long memberId) {
		if (memberId == null) {
			throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
		}

		List<MemberSearchHistory> results = memberSearchHistoryQueryRepository.findRecentSearches(
			memberId,
			null,
			DEFAULT_PAGE_SIZE);

		List<RecentSearchItem> data = results.stream()
			.map(history -> new RecentSearchItem(
				history.getId(),
				history.getKeyword(),
				history.getUpdatedAt()))
			.toList();

		return new OffsetPageResponse<>(
			data,
			new OffsetPagination(0, DEFAULT_PAGE_SIZE, 1, data.size()));
	}

	@Transactional
	public void deleteRecentSearch(Long memberId, Long historyId) {
		if (memberId == null) {
			throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
		}
		MemberSearchHistory history = memberSearchHistoryRepository
			.findByIdAndMemberIdAndDeletedAtIsNull(historyId, memberId)
			.orElseThrow(() -> new BusinessException(SearchErrorCode.RECENT_SEARCH_NOT_FOUND));
		history.delete();
	}

	private CursorPageResponse<SearchRestaurantItem> searchRestaurants(
		String keyword,
		SearchCursor cursor,
		int pageSize) {
		List<Restaurant> result = searchQueryRepository.searchRestaurantsByKeyword(
			keyword,
			cursor,
			pageSize + 1);

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

		List<SearchRestaurantItem> items = pageContent.stream()
			.map(restaurant -> new SearchRestaurantItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getFullAddress(),
				thumbnailUrl(thumbnails.getOrDefault(restaurant.getId(), List.of()))))
			.toList();

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				nextCursor,
				hasNext,
				pageSize));
	}

	private List<SearchGroupSummary> searchGroups(String keyword, int pageSize) {
		List<Group> groups = groupQueryRepository.searchByKeyword(
			keyword,
			GroupStatus.ACTIVE,
			pageSize);

		List<Long> groupIds = groups.stream()
			.map(Group::getId)
			.toList();

		Map<Long, Long> memberCounts = groupIds.isEmpty()
			? Map.of()
			: groupMemberRepository.findMemberCounts(groupIds).stream()
				.collect(Collectors.toMap(
					GroupMemberCountProjection::getGroupId,
					GroupMemberCountProjection::getMemberCount));

		return groups.stream()
			.map(group -> new SearchGroupSummary(
				group.getId(),
				group.getName(),
				group.getLogoImageUrl(),
				memberCounts.getOrDefault(group.getId(), 0L)))
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

	private String thumbnailUrl(List<RestaurantImageDto> images) {
		if (images == null || images.isEmpty()) {
			return null;
		}
		return images.getFirst().url();
	}

}
