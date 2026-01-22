package com.tasteam.domain.review.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.review.dto.ReviewCursor;
import com.tasteam.domain.review.dto.ReviewQueryDto;
import com.tasteam.domain.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	long countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull(Long restaurantId);

	long countByRestaurantIdAndIsRecommendedFalseAndDeletedAtIsNull(Long restaurantId);

	@Query("""
			select distinct r.restaurant.id
			from Review r
			where r.member.id in :memberIds
			  and r.deletedAt is null
		""")
	List<Long> findReviewedRestaurantIdsByMemberIds(List<Long> memberIds);

	List<Review> findByRestaurantIdAndDeletedAtIsNull(long restaurantId);

	@Query("""
		select new com.tasteam.domain.review.dto.ReviewQueryDto(
		    r.id,
		    m.id,
		    m.nickname,
		    r.content,
		    r.isRecommended,
		    r.createdAt
		)
		from Review r
		join r.member m
		where r.restaurant.id = :restaurantId
		  and r.deletedAt is null
		  and (
		        :#{#cursor} is null
		        or r.createdAt < :#{#cursor.createdAt}
		        or (
		            r.createdAt = :#{#cursor.createdAt}
		            and r.id < :#{#cursor.id}
		        )
		  )
		order by r.createdAt desc, r.id desc""")
	List<ReviewQueryDto> findReviewFlatsByRestaurantIdWithCursor(
		@Param("restaurantId")
		Long restaurantId,
		@Param("cursor")
		ReviewCursor cursor,
		Pageable pageable);

}
