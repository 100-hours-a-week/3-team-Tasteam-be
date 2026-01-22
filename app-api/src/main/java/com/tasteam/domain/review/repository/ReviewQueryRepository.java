package com.tasteam.domain.review.repository;

import java.util.List;

import com.tasteam.domain.review.dto.ReviewCursor;
import com.tasteam.domain.review.dto.ReviewQueryDto;

public interface ReviewQueryRepository {

	List<ReviewQueryDto> findRestaurantReviews(
		Long restaurantId,
		ReviewCursor cursor,
		int size);
}
