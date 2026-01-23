package com.tasteam.domain.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.review.entity.ReviewImage;
import com.tasteam.domain.review.repository.projection.ReviewImageProjection;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

	@Query("""
		select
			ri.review.id as reviewId,
			ri.id as imageId,
			ri.imageUrl as imageUrl
		from ReviewImage ri
		where ri.review.id in :reviewIds
		  and ri.deletedAt is null
		order by ri.review.id asc, ri.id asc
		""")
	List<ReviewImageProjection> findReviewImages(
		@Param("reviewIds")
		List<Long> reviewIds);

	List<ReviewImage> findByReview_IdAndDeletedAtIsNull(Long reviewId);
}
