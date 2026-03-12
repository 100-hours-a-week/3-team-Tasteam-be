package com.tasteam.domain.search.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.RecentSearchItem;
import com.tasteam.domain.search.dto.response.SearchGroupSummary;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.domain.search.dto.response.SearchRestaurantItem;
import com.tasteam.domain.search.entity.MemberSearchHistory;
import com.tasteam.domain.search.event.SearchCompletedEvent;
import com.tasteam.domain.search.event.SearchEventPublisher;
import com.tasteam.domain.search.repository.MemberSearchHistoryQueryRepository;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.SearchErrorCode;
import com.tasteam.global.utils.CursorPageBuilder;
import com.tasteam.global.validation.KeywordSecurityPolicy;

@Service
public class SearchService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int THUMBNAIL_LIMIT = 3;
	private static final double DEFAULT_RADIUS_KM = 3.0;
	private static final long SEARCH_QUERY_TIMEOUT_SECONDS = 3L;

	private final FileService fileService;
	private final MemberSearchHistoryRepository memberSearchHistoryRepository;
	private final MemberSearchHistoryQueryRepository memberSearchHistoryQueryRepository;
	private final SearchDataService searchDataService;
	private final SearchEventPublisher searchEventPublisher;
	private final Executor searchQueryExecutor;

	public SearchService(
		FileService fileService,
		MemberSearchHistoryRepository memberSearchHistoryRepository,
		MemberSearchHistoryQueryRepository memberSearchHistoryQueryRepository,
		SearchDataService searchDataService,
		SearchEventPublisher searchEventPublisher,
		@Qualifier("searchQueryExecutor")
		Executor searchQueryExecutor) {
		this.fileService = fileService;
		this.memberSearchHistoryRepository = memberSearchHistoryRepository;
		this.memberSearchHistoryQueryRepository = memberSearchHistoryQueryRepository;
		this.searchDataService = searchDataService;
		this.searchEventPublisher = searchEventPublisher;
		this.searchQueryExecutor = searchQueryExecutor;
	}

	public SearchResponse search(Long memberId, SearchRequest request) {
		String keyword = request.keyword().trim();
		validateSearchKeyword(keyword);
		int pageSize = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
		Double radiusMeters = resolveRadiusMeters(request);

		CompletableFuture<SearchDataService.GroupData> groupFuture = CompletableFuture.supplyAsync(
			() -> searchDataService.fetchGroups(keyword, pageSize), searchQueryExecutor);
		CompletableFuture<SearchDataService.RestaurantPageData> restaurantFuture = CompletableFuture.supplyAsync(
			() -> searchDataService.fetchRestaurants(
				keyword, request.cursor(), pageSize,
				request.latitude(), request.longitude(), radiusMeters),
			searchQueryExecutor);

		SearchDataService.GroupData groupData;
		SearchDataService.RestaurantPageData restaurantData;
		try {
			groupData = groupFuture.get(SEARCH_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			restaurantData = restaurantFuture.get(SEARCH_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		} catch (TimeoutException e) {
			groupFuture.cancel(true);
			restaurantFuture.cancel(true);
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof BusinessException be) {
				throw be;
			}
			throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}

		Map<Long, List<DomainImageItem>> groupLogos = groupData.groupIds().isEmpty()
			? Map.of()
			: fileService.getDomainImageUrls(DomainType.GROUP, groupData.groupIds());
		Map<Long, List<DomainImageItem>> restaurantDomainImages = restaurantData.restaurantIds().isEmpty()
			? Map.of()
			: fileService.getDomainImageUrls(DomainType.RESTAURANT, restaurantData.restaurantIds());

		List<SearchGroupSummary> groups = groupData.groups().stream()
			.map(group -> new SearchGroupSummary(
				group.getId(),
				group.getName(),
				firstDomainImageUrl(groupLogos.getOrDefault(group.getId(), List.of())),
				groupData.memberCounts().getOrDefault(group.getId(), 0L)))
			.toList();

		Map<Long, List<RestaurantImageDto>> thumbnails = buildThumbnails(restaurantDomainImages);
		CursorPageBuilder.Page<SearchRestaurantCursorRow> restaurantPage = restaurantData.page();
		List<SearchRestaurantItem> restaurantItems = restaurantPage.items().stream()
			.map(SearchRestaurantCursorRow::restaurant)
			.map(restaurant -> new SearchRestaurantItem(
				restaurant.getId(),
				restaurant.getName(),
				restaurant.getFullAddress(),
				thumbnailUrl(thumbnails.getOrDefault(restaurant.getId(), List.of())),
				restaurantData.categories().getOrDefault(restaurant.getId(), List.of())))
			.toList();

		CursorPageResponse<SearchRestaurantItem> restaurants = new CursorPageResponse<>(
			restaurantItems,
			new CursorPageResponse.Pagination(
				restaurantPage.nextCursor(),
				restaurantPage.hasNext(),
				restaurantPage.size()));

		searchEventPublisher.publish(new SearchCompletedEvent(
			memberId,
			keyword,
			groups.size(),
			restaurantItems.size()));

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

	private Double resolveRadiusMeters(SearchRequest request) {
		if (request.latitude() == null || request.longitude() == null) {
			return null;
		}
		return (request.radiusKm() == null ? DEFAULT_RADIUS_KM : request.radiusKm()) * 1000.0;
	}

	private Map<Long, List<RestaurantImageDto>> buildThumbnails(Map<Long, List<DomainImageItem>> domainImages) {
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

	private void validateSearchKeyword(String keyword) {
		if (!KeywordSecurityPolicy.isSafeKeyword(keyword)) {
			throw new BusinessException(SearchErrorCode.INVALID_SEARCH_KEYWORD);
		}
	}

}
