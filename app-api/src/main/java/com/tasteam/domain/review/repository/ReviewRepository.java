package com.tasteam.domain.review.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@EntityGraph(attributePaths = {"restaurant", "member"})
	Page<Review> findAllByDeletedAtIsNull(Pageable pageable);

	@EntityGraph(attributePaths = {"restaurant", "member"})
	Page<Review> findAllByRestaurant_IdAndDeletedAtIsNull(Long restaurantId, Pageable pageable);

	java.util.Optional<Review> findByIdAndDeletedAtIsNull(Long id);

	List<Review> findByIdInAndDeletedAtIsNull(Iterable<Long> ids);

	/**
	 * 해당 레스토랑 소속·삭제되지 않은 리뷰만 vector_synced_at 갱신.
	 *
	 * @param reviewIds   갱신할 리뷰 ID 목록
	 * @param restaurantId 레스토랑 ID
	 * @return 업데이트된 행 수
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Review r SET r.vectorSyncedAt = :syncedAt WHERE r.id IN :reviewIds AND r.restaurant.id = :restaurantId AND r.deletedAt IS NULL")
	int markVectorSyncedByIdsAndRestaurant(
		@Param("reviewIds")
		List<Long> reviewIds,
		@Param("restaurantId")
		Long restaurantId,
		@Param("syncedAt")
		Instant syncedAt);
}
