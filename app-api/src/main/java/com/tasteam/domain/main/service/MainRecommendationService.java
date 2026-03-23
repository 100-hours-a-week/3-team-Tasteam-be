package com.tasteam.domain.main.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModel;
import com.tasteam.domain.recommendation.entity.RestaurantRecommendationModelStatus;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationJdbcRepository;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationRow;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationModelRepository;
import com.tasteam.domain.restaurant.policy.RestaurantSearchPolicy;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainRecommendationService {

	private static final int HOME_RECOMMEND_LIMIT = 5;
	private static final Logger log = LoggerFactory.getLogger(MainRecommendationService.class);

	private final RestaurantRecommendationModelRepository modelRepository;
	private final RestaurantRecommendationJdbcRepository recommendationJdbcRepository;
	private final MainDataService mainDataService;

	public List<MainRestaurantDistanceProjection> fetchRecommendSection(
		Long memberId,
		Double latitude,
		Double longitude) {

		if (memberId == null) {
			return List.of();
		}

		try {
			Optional<RestaurantRecommendationModel> model = findServingModel();
			if (model.isEmpty() || model.get().getId() == null) {
				return List.of();
			}

			List<RestaurantRecommendationRow> rows = recommendationJdbcRepository.findTopByMemberIdAndModelId(
				memberId,
				model.get().getId(),
				HOME_RECOMMEND_LIMIT);
			if (rows.isEmpty()) {
				return List.of();
			}

			List<Long> restaurantIds = rows.stream()
				.map(RestaurantRecommendationRow::restaurantId)
				.distinct()
				.limit(RestaurantSearchPolicy.SECTION_SIZE)
				.toList();

			if (restaurantIds.isEmpty()) {
				return List.of();
			}

			if (latitude != null && longitude != null) {
				return mainDataService.fetchRestaurantsByIdsWithDistance(restaurantIds, latitude, longitude);
			}
			return mainDataService.fetchRestaurantsByIds(restaurantIds);
		} catch (DataAccessException ex) {
			log.warn("Skip recommendation home section because recommendation read model is unavailable", ex);
			return List.of();
		}
	}

	private Optional<RestaurantRecommendationModel> findServingModel() {
		return modelRepository.findByStatus(RestaurantRecommendationModelStatus.ACTIVE)
			.or(() -> modelRepository.findAllByOrderByCreatedAtDesc().stream()
				.filter(model -> model.getStatus() == RestaurantRecommendationModelStatus.READY)
				.findFirst());
	}
}
