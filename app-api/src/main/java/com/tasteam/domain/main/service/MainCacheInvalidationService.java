package com.tasteam.domain.main.service;

import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainCacheInvalidationService {

	private static final List<String> MAIN_SECTION_CACHE_NAMES = List.of(
		"main-section-hot-all",
		"main-section-new-all",
		"main-section-ai-all",
		"main-section-hot-geo",
		"main-section-new-geo",
		"main-section-ai-geo");

	private static final List<String> RESTAURANT_METADATA_CACHE_NAMES = List.of(
		"restaurant-location",
		"restaurant-categories",
		"restaurant-thumbnail",
		"restaurant-summary");

	private final CacheManager cacheManager;

	public void evictHomeCaches(Long restaurantId) {
		evictMainSectionCaches();
		evictRestaurantMetadataCaches(restaurantId);
	}

	public void evictMainSectionCaches() {
		MAIN_SECTION_CACHE_NAMES.forEach(this::clearCache);
	}

	public void evictRestaurantMetadataCaches(Long restaurantId) {
		if (restaurantId == null) {
			return;
		}
		RESTAURANT_METADATA_CACHE_NAMES.forEach(cacheName -> evictCacheKey(cacheName, restaurantId));
	}

	private void clearCache(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			log.debug("cache clear skipped. cache not found. cache={}", cacheName);
			return;
		}
		cache.clear();
		log.info("cache cleared. cache={}", cacheName);
	}

	private void evictCacheKey(String cacheName, Long key) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			log.debug("cache evict skipped. cache not found. cache={}, key={}", cacheName, key);
			return;
		}
		cache.evict(key);
		log.info("cache evicted. cache={}, key={}", cacheName, key);
	}
}
