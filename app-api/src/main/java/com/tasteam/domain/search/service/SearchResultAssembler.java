package com.tasteam.domain.search.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.dto.response.SearchGroupSummary;
import com.tasteam.domain.search.dto.response.SearchRestaurantItem;

@Component
public class SearchResultAssembler {

	private static final int THUMBNAIL_LIMIT = 3;

	public List<SearchGroupSummary> buildGroupSummaries(
		SearchDataService.GroupData groupData,
		Map<Long, List<DomainImageItem>> groupLogos) {
		return groupData.groups().stream()
			.map(group -> new SearchGroupSummary(
				group.getId(),
				group.getName(),
				firstImageUrl(groupLogos.getOrDefault(group.getId(), List.of()), DomainImageItem::url)))
			.toList();
	}

	public List<SearchRestaurantItem> buildRestaurantItems(
		SearchDataService.RestaurantPageData restaurantData,
		Map<Long, List<DomainImageItem>> restaurantDomainImages) {
		Map<Long, List<RestaurantImageDto>> thumbnails = buildThumbnails(restaurantDomainImages);
		return restaurantData.page().items().stream()
			.map(SearchRestaurantCursorRow::restaurant)
			.map(r -> new SearchRestaurantItem(
				r.getId(),
				r.getName(),
				r.getFullAddress(),
				firstImageUrl(thumbnails.getOrDefault(r.getId(), List.of()), RestaurantImageDto::url),
				restaurantData.categories().getOrDefault(r.getId(), List.of())))
			.toList();
	}

	private Map<Long, List<RestaurantImageDto>> buildThumbnails(
		Map<Long, List<DomainImageItem>> domainImages) {
		return domainImages.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().stream()
					.limit(THUMBNAIL_LIMIT)
					.map(img -> new RestaurantImageDto(img.imageId(), img.url()))
					.toList()));
	}

	private <T> String firstImageUrl(List<T> images, Function<T, String> urlExtractor) {
		if (images == null || images.isEmpty()) {
			return null;
		}
		return urlExtractor.apply(images.getFirst());
	}
}
