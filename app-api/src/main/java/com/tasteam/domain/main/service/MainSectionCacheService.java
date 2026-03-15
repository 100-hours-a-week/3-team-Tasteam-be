package com.tasteam.domain.main.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.main.repository.MainRestaurantRepository;
import com.tasteam.domain.restaurant.policy.RestaurantSearchPolicy;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainSectionCacheService {

	private static final List<Long> SENTINEL_EXCLUDE = List.of(-1L);

	private final MainRestaurantRepository restaurantRepository;

	@Cacheable(cacheNames = "main-section-hot-geo", key = "T(String).format('%d_%d', T(Math).round(#lat * 1000), T(Math).round(#lon * 1000))")
	@Transactional(readOnly = true)
	public List<Long> fetchHotSectionIdsByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findHotRestaurants(la, lo, radius, limit))
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-new-geo", key = "T(String).format('%d_%d', T(Math).round(#lat * 1000), T(Math).round(#lon * 1000))")
	@Transactional(readOnly = true)
	public List<Long> fetchNewSectionIdsByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findNewRestaurants(la, lo, radius, limit))
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-ai-geo", key = "T(String).format('%d_%d', T(Math).round(#lat * 1000), T(Math).round(#lon * 1000))")
	@Transactional(readOnly = true)
	public List<Long> fetchAiSectionIdsByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findAiRecommendRestaurants(la, lo, radius, limit))
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-hot-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchHotSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findHotRestaurantsAll(excludeIds, limit));
	}

	@Cacheable(cacheNames = "main-section-new-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchNewSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findNewRestaurantsAll(excludeIds, limit));
	}

	@Cacheable(cacheNames = "main-section-ai-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchAiSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findAiRecommendRestaurantsAll(excludeIds, limit));
	}

	private List<MainRestaurantDistanceProjection> fetchWithRadiusExpansion(
		double lat, double lon, LocationQuery query) {
		List<MainRestaurantDistanceProjection> results = query.execute(
			lat, lon, RestaurantSearchPolicy.SECTION_RADIUS_METER, RestaurantSearchPolicy.SECTION_SIZE);

		if (results.size() >= RestaurantSearchPolicy.SECTION_SIZE) {
			return results;
		}

		LinkedHashMap<Long, MainRestaurantDistanceProjection> collected = new LinkedHashMap<>();
		results.forEach(r -> collected.put(r.getId(), r));
		fillWithRandom(collected, RestaurantSearchPolicy.SECTION_SIZE - collected.size());
		return new ArrayList<>(collected.values());
	}

	private List<MainRestaurantDistanceProjection> fetchWithoutLocation(NoLocationQuery query) {
		List<MainRestaurantDistanceProjection> results = query.execute(SENTINEL_EXCLUDE,
			RestaurantSearchPolicy.SECTION_SIZE);

		if (results.size() >= RestaurantSearchPolicy.SECTION_SIZE) {
			return results;
		}

		LinkedHashMap<Long, MainRestaurantDistanceProjection> collected = new LinkedHashMap<>();
		results.forEach(r -> collected.put(r.getId(), r));
		fillWithRandom(collected, RestaurantSearchPolicy.SECTION_SIZE - collected.size());
		return new ArrayList<>(collected.values());
	}

	private void fillWithRandom(
		LinkedHashMap<Long, MainRestaurantDistanceProjection> collected, int needed) {
		if (needed <= 0) {
			return;
		}
		List<Long> excludeIds = collected.isEmpty() ? SENTINEL_EXCLUDE : new ArrayList<>(collected.keySet());
		restaurantRepository.findRandomRestaurants(excludeIds, needed)
			.forEach(r -> collected.putIfAbsent(r.getId(), r));
	}

	@FunctionalInterface
	private interface LocationQuery {
		List<MainRestaurantDistanceProjection> execute(double lat, double lon, int radius, int limit);
	}

	@FunctionalInterface
	private interface NoLocationQuery {
		List<MainRestaurantDistanceProjection> execute(List<Long> excludeIds, int limit);
	}
}
