package com.tasteam.domain.review.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tasteam.domain.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	long countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull(Long restaurantId);

	long countByRestaurantIdAndIsRecommendedFalseAndDeletedAtIsNull(Long restaurantId);

	long countByRestaurantIdAndDeletedAtIsNull(Long restaurantId);

	List<Review> findByRestaurantIdAndDeletedAtIsNull(long restaurantId);

	List<Review> findByRestaurantIdAndDeletedAtIsNull(long restaurantId, Pageable pageable);

	@Query("""
		select distinct r.restaurant.id
		from Review r
		where r.deletedAt is null
		""")
	List<Long> findDistinctRestaurantIdsByDeletedAtIsNull();

	java.util.Optional<Review> findByIdAndDeletedAtIsNull(Long id);
}
