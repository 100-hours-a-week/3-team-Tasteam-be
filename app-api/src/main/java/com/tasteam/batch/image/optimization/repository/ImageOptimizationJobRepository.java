package com.tasteam.batch.image.optimization.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.batch.image.optimization.entity.ImageOptimizationJob;
import com.tasteam.batch.image.optimization.entity.OptimizationJobStatus;

public interface ImageOptimizationJobRepository extends JpaRepository<ImageOptimizationJob, Long> {

	List<ImageOptimizationJob> findByStatusOrderByCreatedAtAsc(OptimizationJobStatus status, Pageable pageable);

	@Query("""
		SELECT j.image.id FROM ImageOptimizationJob j
		WHERE j.image.id IN :imageIds
		  AND j.status IN ('PENDING', 'SUCCESS')
		""")
	Set<Long> findAlreadyEnqueuedImageIds(@Param("imageIds")
	Collection<Long> imageIds);

	@Query("""
		SELECT j FROM ImageOptimizationJob j
		WHERE j.image.id IN :imageIds
		  AND j.status IN ('FAILED', 'SKIPPED')
		""")
	List<ImageOptimizationJob> findRetryableJobsByImageIds(@Param("imageIds")
	Collection<Long> imageIds);

	@Modifying
	@Query("""
		DELETE FROM ImageOptimizationJob j
		WHERE j.status = 'SUCCESS'
		  AND j.processedAt < :cutoff
		""")
	int deleteBySuccessAndProcessedAtBefore(@Param("cutoff")
	Instant cutoff);
}
