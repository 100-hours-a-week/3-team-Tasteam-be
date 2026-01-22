package com.tasteam.domain.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.review.entity.ReviewKeyword;
import com.tasteam.domain.review.repository.projection.ReviewKeywordProjection;

public interface ReviewKeywordRepository extends JpaRepository<ReviewKeyword, Long> {

	@Query("""
		select
			rk.review.id as reviewId,
			k.name as keywordName
		from ReviewKeyword rk
		join rk.keyword k
		where rk.review.id in :reviewIds
		order by rk.review.id asc, k.id asc
		""")
	List<ReviewKeywordProjection> findReviewKeywords(
		@Param("reviewIds")
		List<Long> reviewIds);
}
