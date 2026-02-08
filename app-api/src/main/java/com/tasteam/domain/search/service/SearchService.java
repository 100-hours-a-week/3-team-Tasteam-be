package com.tasteam.domain.search.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.repository.GroupQueryRepository;
import com.tasteam.domain.group.repository.projection.GroupMemberCountProjection;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
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
import com.tasteam.global.utils.CursorPageBuilder;

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
	private final FileService fileService;
	private final MemberSearchHistoryRepository memberSearchHistoryRepository;
	private final MemberSearchHistoryQueryRepository memberSearchHistoryQueryRepository;
	private final SearchQueryRepository searchQueryRepository;
	private final CursorCodec cursorCodec;
	private final SearchHistoryRecorder searchHistoryRecorder;

	@Transactional(readOnly = true)
	public SearchResponse search(Long memberId, SearchRequest request) {
		String keyword = request.keyword().trim();
		int pageSize = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
		CursorPageBuilder<SearchCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, request.cursor(),
			SearchCursor.class);
		if (pageBuilder.isInvalid()) {
			return SearchResponse.emptyResponse();
		}

		List<SearchGroupSummary> groups = searchGroups(keyword, pageSize);
		CursorPageResponse<SearchRestaurantItem> restaurants = searchRestaurants(keyword, pageBuilder, pageSize,
			request.latitude(), request.longitude());

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
		String keyword, CursorPageBuilder<SearchCursor> pageBuilder, int pageSize, Double latitude,
		Double longitude) {
		List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
			keyword,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(pageSize),
			latitude,
			longitude);

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

		Map<Long, List<RestaurantImageDto>> thumbnails = findRestaurantThumbnails(restaurantIds);

		List<SearchRestaurantItem> items = page.items().stream()
			.map(SearchRestaurantCursorRow::restaurant)
			.map(restaurant -> new SearchRestaurantItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getFullAddress(),
				thumbnailUrl(thumbnails.getOrDefault(restaurant.getId(), List.of()))))
			.toList();

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(
				page.nextCursor(),
				page.hasNext(),
				page.size()));
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

		Map<Long, List<DomainImageItem>> logos = groupIds.isEmpty()
			? Map.of()
			: fileService.getDomainImageUrls(DomainType.GROUP, groupIds);

		return groups.stream()
			.map(group -> new SearchGroupSummary(
				group.getId(),
				group.getName(),
				firstDomainImageUrl(logos.getOrDefault(group.getId(), List.of())),
				memberCounts.getOrDefault(group.getId(), 0L)))
			.toList();
	}

	private Map<Long, List<RestaurantImageDto>> findRestaurantThumbnails(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, List<DomainImageItem>> domainImages = fileService.getDomainImageUrls(
			DomainType.RESTAURANT,
			restaurantIds);

		return domainImages.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> {
					List<DomainImageItem> images = entry.getValue();
					List<DomainImageItem> limited = images.size() > THUMBNAIL_LIMIT
						? images.subList(0, THUMBNAIL_LIMIT)
						: images;
					return limited.stream()
						.map(img -> new RestaurantImageDto(img.imageId(), img.url()))
						.toList();
				}));
	}

	private String thumbnailUrl(List<RestaurantImageDto> images) {
		if (images == null || images.isEmpty()) {
			return null;
		}
		return images.getFirst().url();
	}

	private String firstDomainImageUrl(List<DomainImageItem> images) {
		if (images == null || images.isEmpty()) {
			return null;
		}
		return images.getFirst().url();
	}

}
