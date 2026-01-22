package com.tasteam.domain.review.service;

import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.global.utils.CursorCodec;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewService {

	private final RestaurantRepository restaurantRepository;
	private final CursorCodec cursorCodec;
	private final ReviewRepository reviewRepository;

	public CursorPageResponse<ReviewResponse> getRestaurantReviews(
		long restaurantId,
		RestaurantReviewListRequest request) {
		/*
		if (!restaurantRepository.existsByIdAndDeletedAtIsNull(restaurantId)) {
		    throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
		}
		
		ReviewCursor cursor = cursorCodec.decode(request.cursor(), ReviewCursor.class);
		
		List<Review> reviewList = reviewRepository.findByRestaurantIdAndDeletedAtIsNull(restaurantId);
		
		List<ReviewResponse> items = reviewList.stream()
		        .map(review -> new ReviewResponse(
		                review.getId(),
		                new ReviewResponse.AuthorResponse(review.getMember().getId()),
		                review.getContent(),
		                review.isRecommended(),
		                keywords,
		                thumbnails,
		                review.getCreatedAt()
		        )).toList();
		
		return new CursorPageResponse<>(
		        items,
		        new CursorPageResponse.Pagination(
		                result.nextCursor(),
		                result.hasNext(),
		                result.size()
		        )
		);
		 */
		return CursorPageResponse.empty();
	}
}
