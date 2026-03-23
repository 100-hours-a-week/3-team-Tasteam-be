package com.tasteam.domain.review.repository;

import java.util.List;

import com.tasteam.domain.review.dto.ReviewCursor;
import com.tasteam.domain.review.dto.ReviewDetailQueryDto;
import com.tasteam.domain.review.dto.ReviewMemberQueryDto;
import com.tasteam.domain.review.dto.ReviewQueryDto;

public interface ReviewQueryRepository {

	List<ReviewQueryDto> findRestaurantReviews(
		Long restaurantId,
		ReviewCursor cursor,
		int size);

	List<ReviewQueryDto> findGroupReviews(
		Long groupId,
		ReviewCursor cursor,
		int size);

	List<ReviewQueryDto> findSubgroupReviews(
		Long subgroupId,
		ReviewCursor cursor,
		int size);

	List<ReviewMemberQueryDto> findMemberReviews(
		Long memberId,
		ReviewCursor cursor,
		int size);

	ReviewDetailQueryDto findReviewDetail(Long reviewId);
}
