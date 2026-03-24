package com.tasteam.domain.main.service;

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

	@Cacheable(cacheNames = "main-section-hot-geo", key = "T(String).format('%d_%d', T(Math).round(#lat * 100), T(Math).round(#lon * 100))", unless = "#result.isEmpty()")
	@Transactional(readOnly = true)
	public List<Long> fetchHotSectionIdsByLocation(double lat, double lon) {
		return restaurantRepository.findHotRestaurants(lat, lon, RestaurantSearchPolicy.SECTION_SIZE)
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-new-geo", key = "T(String).format('%d_%d', T(Math).round(#lat * 100), T(Math).round(#lon * 100))", unless = "#result.isEmpty()")
	@Transactional(readOnly = true)
	public List<Long> fetchNewSectionIdsByLocation(double lat, double lon) {
		return restaurantRepository.findNewRestaurants(lat, lon, RestaurantSearchPolicy.SECTION_SIZE)
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-ai-geo", key = "T(String).format('%d_%d', T(Math).round(#lat * 100), T(Math).round(#lon * 100))", unless = "#result.isEmpty()")
	@Transactional(readOnly = true)
	public List<Long> fetchAiSectionIdsByLocation(double lat, double lon) {
		return restaurantRepository.findAiRecommendRestaurants(lat, lon, RestaurantSearchPolicy.SECTION_SIZE)
			.stream().map(MainRestaurantDistanceProjection::getId).toList();
	}

	@Cacheable(cacheNames = "main-section-hot-all", key = "'all'", unless = "#result.isEmpty()")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchHotSectionAll() {
		return restaurantRepository.findHotRestaurantsAll(SENTINEL_EXCLUDE, RestaurantSearchPolicy.SECTION_SIZE);
	}

	@Cacheable(cacheNames = "main-section-new-all", key = "'all'", unless = "#result.isEmpty()")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchNewSectionAll() {
		return restaurantRepository.findNewRestaurantsAll(SENTINEL_EXCLUDE, RestaurantSearchPolicy.SECTION_SIZE);
	}

	@Cacheable(cacheNames = "main-section-ai-all", key = "'all'", unless = "#result.isEmpty()")
	@Transactional(readOnly = true)
	public List<MainRestaurantDistanceProjection> fetchAiSectionAll() {
		return restaurantRepository.findAiRecommendRestaurantsAll(SENTINEL_EXCLUDE,
			RestaurantSearchPolicy.SECTION_SIZE);
	}

}
