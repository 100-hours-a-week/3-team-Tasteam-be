package com.tasteam.domain.main.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.policy.RestaurantSearchPolicy;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantLocationProjection;
import com.tasteam.domain.restaurant.support.GeoUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainDataService {

	private static final List<Long> SENTINEL_EXCLUDE = List.of(-1L);

	private final RestaurantRepository restaurantRepository;
	private final CacheManager cacheManager;

	@Lazy
	@Autowired
	private MainDataService self;

	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchHotSectionByLocation(double lat, double lon) {
		List<Long> ids = self.fetchHotSectionIdsByLocation(lat, lon);
		return fetchDistancesWithCoordCache(ids, lat, lon);
	}

	@Cacheable(cacheNames = "main-section-hot-geo", key = "T(String).format('%d_%d', (long)(#lat * 1000), (long)(#lon * 1000))")
	@Transactional(readOnly = true)
	public List<Long> fetchHotSectionIdsByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findHotRestaurants(la, lo, radius, limit))
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-hot-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchHotSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findHotRestaurantsAll(excludeIds, limit));
	}

	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchNewSectionByLocation(double lat, double lon) {
		List<Long> ids = self.fetchNewSectionIdsByLocation(lat, lon);
		return fetchDistancesWithCoordCache(ids, lat, lon);
	}

	@Cacheable(cacheNames = "main-section-new-geo", key = "T(String).format('%d_%d', (long)(#lat * 1000), (long)(#lon * 1000))")
	@Transactional(readOnly = true)
	public List<Long> fetchNewSectionIdsByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findNewRestaurants(la, lo, radius, limit))
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-new-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchNewSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findNewRestaurantsAll(excludeIds, limit));
	}

	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchAiSectionByLocation(double lat, double lon) {
		List<Long> ids = self.fetchAiSectionIdsByLocation(lat, lon);
		return fetchDistancesWithCoordCache(ids, lat, lon);
	}

	@Cacheable(cacheNames = "main-section-ai-geo", key = "T(String).format('%d_%d', (long)(#lat * 1000), (long)(#lon * 1000))")
	@Transactional(readOnly = true)
	public List<Long> fetchAiSectionIdsByLocation(double lat, double lon) {
		return fetchWithRadiusExpansion(lat, lon,
			(la, lo, radius, limit) -> restaurantRepository.findAiRecommendRestaurants(la, lo, radius, limit))
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-ai-all", key = "'all'")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchAiSectionAll() {
		return fetchWithoutLocation(
			(excludeIds, limit) -> restaurantRepository.findAiRecommendRestaurantsAll(excludeIds, limit));
	}

	private List<MainRestaurantDistanceProjection> fetchDistancesWithCoordCache(
		List<Long> ids, double userLat, double userLon) {

		Cache locationCache = cacheManager.getCache("restaurant-location");
		if (locationCache == null) {
			return restaurantRepository.findDistancesByIds(ids, userLat, userLon);
		}

		Map<Long, CachedLocation> coordMap = new LinkedHashMap<>();
		List<Long> missIds = new ArrayList<>();

		for (Long id : ids) {
			CachedLocation cached = locationCache.get(id, CachedLocation.class);
			if (cached != null) {
				coordMap.put(id, cached);
			} else {
				missIds.add(id);
			}
		}

		if (!missIds.isEmpty()) {
			for (RestaurantLocationProjection p : restaurantRepository.findLocationsByIds(missIds)) {
				CachedLocation cached = new CachedLocation(p.getName(), p.getLatitude(), p.getLongitude());
				locationCache.put(p.getId(), cached);
				coordMap.put(p.getId(), cached);
			}
		}

		return ids.stream()
			.map(id -> {
				CachedLocation loc = coordMap.get(id);
				if (loc == null) {
					return null;
				}
				double dist = GeoUtils.distanceMeter(userLat, userLon, loc.lat(), loc.lon());
				return (MainRestaurantDistanceProjection)new CachedDistance(id, loc.name(), dist);
			})
			.filter(Objects::nonNull)
			.toList();
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

	private record CachedLocation(String name, double lat, double lon) {
	}

	private record CachedDistance(Long id, String name, Double distanceMeter)
		implements
			MainRestaurantDistanceProjection {
		@Override
		public Long getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Double getDistanceMeter() {
			return distanceMeter;
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
