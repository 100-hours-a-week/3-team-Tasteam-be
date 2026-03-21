package com.tasteam.domain.main.service;

import static com.tasteam.domain.restaurant.service.RestaurantAiJsonParser.extractSummaryText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.main.repository.MainMetadataRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainMetadataLoader {

	private final MainMetadataRepository metadataRepository;
	private final FileService fileService;
	private final CacheManager cacheManager;

	public Map<Long, List<String>> loadCategories(List<Long> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return loadWithCache("restaurant-categories", ids,
			missIds -> metadataRepository.findCategoriesByRestaurantIds(missIds)
				.stream()
				.collect(java.util.stream.Collectors.groupingBy(
					RestaurantCategoryProjection::getRestaurantId,
					java.util.stream.Collectors.mapping(
						RestaurantCategoryProjection::getCategoryName,
						java.util.stream.Collectors.toList()))));
	}

	public Map<Long, String> loadThumbnails(List<Long> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return loadWithCache("restaurant-thumbnail", ids, missIds -> {
			Map<Long, String> result = new LinkedHashMap<>();
			fileService.getDomainImageUrls(DomainType.RESTAURANT, missIds)
				.forEach((id, images) -> {
					if (images != null && !images.isEmpty()) {
						result.put(id, images.getFirst().url());
					}
				});
			return result;
		});
	}

	public Map<Long, String> loadSummaries(List<Long> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return loadWithCache("restaurant-summary", ids, missIds -> {
			Map<Long, String> result = new LinkedHashMap<>();
			metadataRepository.findSummariesByRestaurantIdIn(missIds)
				.forEach(summary -> {
					String text = extractSummaryText(summary.getSummaryJson());
					if (text != null) {
						result.put(summary.getRestaurantId(), text);
					}
				});
			return result;
		});
	}

	@SuppressWarnings("unchecked")
	private <V> Map<Long, V> loadWithCache(
		String cacheName,
		List<Long> ids,
		Function<List<Long>, Map<Long, V>> dbLoader) {

		Cache cache = cacheManager.getCache(cacheName);
		Map<Long, V> result = new LinkedHashMap<>();
		List<Long> missIds = new ArrayList<>();

		for (Long id : ids) {
			V cached = (cache != null) ? cache.get(id, (Class<V>)Object.class) : null;
			if (cached != null) {
				result.put(id, cached);
			} else {
				missIds.add(id);
			}
		}

		if (!missIds.isEmpty()) {
			Map<Long, V> fetched = dbLoader.apply(missIds);
			fetched.forEach((id, value) -> {
				if (cache != null) {
					cache.put(id, value);
				}
				result.put(id, value);
			});
			// missIds 중 DB에도 없는 항목은 result에 포함하지 않음 (기존 동작 유지)
		}

		return result;
	}
}
