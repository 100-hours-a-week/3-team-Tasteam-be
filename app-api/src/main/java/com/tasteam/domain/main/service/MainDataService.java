package com.tasteam.domain.main.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.policy.RestaurantSearchPolicy;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainDataService {

	private static final List<Long> SENTINEL_EXCLUDE = List.of(-1L);

	private final RestaurantRepository restaurantRepository;

	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchHotSectionByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findHotRestaurants(la, lo, radius, limit));
	}

	@Cacheable(cacheNames = "main-section-hot-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchHotSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findHotRestaurantsAll(excludeIds, limit));
	}

	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchNewSectionByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findNewRestaurants(la, lo, radius, limit));
	}

	@Cacheable(cacheNames = "main-section-new-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchNewSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findNewRestaurantsAll(excludeIds, limit));
	}

	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchAiSectionByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findAiRecommendRestaurants(la, lo, radius, limit));
	}

	@Cacheable(cacheNames = "main-section-ai-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchAiSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findAiRecommendRestaurantsAll(excludeIds, limit));
	}

	private List<MainRestaurantDistanceProjection> fetchWithRadiusExpansion(
		double lat, double lon, LocationQuery query) {
		int maxRadius = RestaurantSearchPolicy.EXPANDED_RADII[RestaurantSearchPolicy.EXPANDED_RADII.length - 1];
		List<MainRestaurantDistanceProjection> results = query.execute(
			lat, lon, maxRadius, RestaurantSearchPolicy.SECTION_SIZE);

		if (results.size() >= RestaurantSearchPolicy.SECTION_SIZE) {
			return results;
		}

		LinkedHashMap<Long, MainRestaurantDistanceProjection> collected = new LinkedHashMap<>();
		results.forEach(r -> collected.put(r.getId(), r));
		fillWithRandom(collected, RestaurantSearchPolicy.SECTION_SIZE - collected.size());
		return new ArrayList<>(collected.values());
	}

	private List<MainRestaurantDistanceProjection> fetchWithoutLocation(NoLocationQuery query) {
		List<Long> excludeIds = SENTINEL_EXCLUDE;
		List<MainRestaurantDistanceProjection> results = query.execute(excludeIds, RestaurantSearchPolicy.SECTION_SIZE);

		if (results.size() >= RestaurantSearchPolicy.SECTION_SIZE) {
			return results;
		}

		LinkedHashMap<Long, MainRestaurantDistanceProjection> collected = new LinkedHashMap<>();
		for (MainRestaurantDistanceProjection r : results) {
			collected.put(r.getId(), r);
		}

		fillWithRandom(collected, RestaurantSearchPolicy.SECTION_SIZE - collected.size());
		return new ArrayList<>(collected.values());
	}

	private void fillWithRandom(LinkedHashMap<Long, MainRestaurantDistanceProjection> collected, int needed) {
		if (needed <= 0) {
			return;
		}
		List<Long> excludeIds = collected.isEmpty() ? SENTINEL_EXCLUDE : new ArrayList<>(collected.keySet());
		List<MainRestaurantDistanceProjection> fillers = restaurantRepository.findRandomRestaurants(
			excludeIds, needed);
		for (MainRestaurantDistanceProjection r : fillers) {
			collected.putIfAbsent(r.getId(), r);
		}
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
