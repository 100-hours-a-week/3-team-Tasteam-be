package com.tasteam.domain.favorite.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.favorite.dto.FavoriteCursor;
import com.tasteam.domain.favorite.dto.FavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantQueryRepository;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.repository.RestaurantImageRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantImageProjection;
import com.tasteam.global.utils.CursorCodec;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final MemberFavoriteRestaurantQueryRepository favoriteQueryRepository;
	private final RestaurantImageRepository restaurantImageRepository;
	private final CursorCodec cursorCodec;

	@Transactional(readOnly = true)
	public CursorPageResponse<FavoriteRestaurantItem> getMyFavoriteRestaurants(Long memberId, String cursorStr) {
		FavoriteCursor cursor = cursorCodec.decodeOrNull(cursorStr, FavoriteCursor.class);

		List<FavoriteRestaurantQueryDto> result = favoriteQueryRepository.findFavoriteRestaurants(
			memberId, cursor, DEFAULT_PAGE_SIZE + 1);

		boolean hasNext = result.size() > DEFAULT_PAGE_SIZE;
		List<FavoriteRestaurantQueryDto> pageContent = hasNext ? result.subList(0, DEFAULT_PAGE_SIZE) : result;

		String nextCursor = null;
		if (hasNext && !pageContent.isEmpty()) {
			FavoriteRestaurantQueryDto last = pageContent.getLast();
			nextCursor = cursorCodec.encode(new FavoriteCursor(last.createdAt(), last.favoriteId()));
		}

		List<Long> restaurantIds = pageContent.stream()
			.map(FavoriteRestaurantQueryDto::restaurantId)
			.toList();

		Map<Long, String> thumbnails = findThumbnails(restaurantIds);

		List<FavoriteRestaurantItem> items = pageContent.stream()
			.map(dto -> new FavoriteRestaurantItem(
				dto.restaurantId(),
				dto.restaurantName(),
				thumbnails.get(dto.restaurantId()),
				dto.createdAt()))
			.toList();

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(nextCursor, hasNext, items.size()));
	}

	private Map<Long, String> findThumbnails(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		return restaurantImageRepository.findRestaurantImages(restaurantIds)
			.stream()
			.collect(groupingBy(
				RestaurantImageProjection::getRestaurantId,
				Collectors.collectingAndThen(
					Collectors.mapping(RestaurantImageProjection::getImageUrl, Collectors.toList()),
					list -> list.isEmpty() ? null : list.getFirst())));
	}
}
