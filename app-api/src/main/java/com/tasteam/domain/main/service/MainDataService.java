package com.tasteam.domain.main.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.main.repository.MainRestaurantRepository;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantLocationProjection;
import com.tasteam.domain.restaurant.support.GeoUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainDataService {

	private final MainRestaurantRepository restaurantRepository;
	private final MainSectionCacheService cacheService;
	private final CacheManager cacheManager;

	public List<MainRestaurantDistanceProjection> fetchHotSectionByLocation(double lat, double lon) {
		List<Long> ids = cacheService.fetchHotSectionIdsByLocation(lat, lon);
		return fetchDistancesWithCoordCache(ids, lat, lon);
	}

	public List<MainRestaurantDistanceProjection> fetchNewSectionByLocation(double lat, double lon) {
		List<Long> ids = cacheService.fetchNewSectionIdsByLocation(lat, lon);
		return fetchDistancesWithCoordCache(ids, lat, lon);
	}

	public List<MainRestaurantDistanceProjection> fetchAiSectionByLocation(double lat, double lon) {
		List<Long> ids = cacheService.fetchAiSectionIdsByLocation(lat, lon);
		return fetchDistancesWithCoordCache(ids, lat, lon);
	}

	public List<MainRestaurantDistanceProjection> fetchHotSectionAll() {
		return cacheService.fetchHotSectionAll();
	}

	public List<MainRestaurantDistanceProjection> fetchNewSectionAll() {
		return cacheService.fetchNewSectionAll();
	}

	public List<MainRestaurantDistanceProjection> fetchAiSectionAll() {
		return cacheService.fetchAiSectionAll();
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
}
