package com.tasteam.batch.ai.vector.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

/**
 * 선점한 Job의 restaurantId로 Restaurant·Review 조회.
 */
@Service
@RequiredArgsConstructor
public class VectorUploadDataLoadService {

	private final RestaurantRepository restaurantRepository;
	private final ReviewRepository reviewRepository;

	/**
	 * 레스토랑과 해당 레스토랑의 리뷰 목록을 조회한다.
	 * 레스토랑이 없거나 삭제된 경우 빈 Optional을 반환한다.
	 *
	 * @param restaurantId AiJob.restaurantId
	 * @return 레스토랑 + 리뷰 목록, 없으면 empty
	 */
	@Transactional(readOnly = true)
	public Optional<RestaurantWithReviews> loadByRestaurantId(Long restaurantId) {
		return restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.map(restaurant -> {
				List<Review> reviews = reviewRepository.findByRestaurantIdAndDeletedAtIsNull(restaurantId);
				return new RestaurantWithReviews(restaurant, reviews);
			});
	}

	public record RestaurantWithReviews(Restaurant restaurant, List<Review> reviews) {
	}
}
