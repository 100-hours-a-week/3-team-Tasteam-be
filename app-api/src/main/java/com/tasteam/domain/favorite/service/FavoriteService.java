package com.tasteam.domain.favorite.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.favorite.dto.FavoriteCursor;
import com.tasteam.domain.favorite.dto.FavoriteRestaurantQueryDto;
import com.tasteam.domain.favorite.dto.response.FavoriteRestaurantItem;
import com.tasteam.domain.favorite.repository.MemberFavoriteRestaurantQueryRepository;
import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.global.utils.CursorCodec;
import com.tasteam.global.utils.CursorPageBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final MemberFavoriteRestaurantQueryRepository favoriteQueryRepository;
	private final FileService fileService;
	private final CursorCodec cursorCodec;

	@Transactional(readOnly = true)
	public CursorPageResponse<FavoriteRestaurantItem> getMyFavoriteRestaurants(Long memberId, String cursorStr) {
		CursorPageBuilder<FavoriteCursor> pageBuilder = CursorPageBuilder.of(cursorCodec, cursorStr,
			FavoriteCursor.class);
		if (pageBuilder.isInvalid()) {
			return CursorPageResponse.empty();
		}

		List<FavoriteRestaurantQueryDto> result = favoriteQueryRepository.findFavoriteRestaurants(
			memberId,
			pageBuilder.cursor(),
			CursorPageBuilder.fetchSize(DEFAULT_PAGE_SIZE));

		CursorPageBuilder.Page<FavoriteRestaurantQueryDto> page = pageBuilder.build(
			result,
			DEFAULT_PAGE_SIZE,
			last -> new FavoriteCursor(last.createdAt(), last.favoriteId()));

		List<Long> restaurantIds = page.items().stream()
			.map(FavoriteRestaurantQueryDto::restaurantId)
			.toList();

		Map<Long, String> thumbnails = findThumbnails(restaurantIds);

		List<FavoriteRestaurantItem> items = page.items().stream()
			.map(dto -> new FavoriteRestaurantItem(
				dto.restaurantId(),
				dto.restaurantName(),
				thumbnails.get(dto.restaurantId()),
				dto.createdAt()))
			.toList();

		return new CursorPageResponse<>(
			items,
			new CursorPageResponse.Pagination(page.nextCursor(), page.hasNext(), page.size()));
	}

	private Map<Long, String> findThumbnails(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<DomainImageItem>> domainImages = fileService.getDomainImageUrls(
			DomainType.RESTAURANT,
			restaurantIds);
		return domainImages.entrySet().stream()
			.filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
			.collect(java.util.stream.Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getFirst().url()));
	}
}
