package com.tasteam.domain.batch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.AiJobType;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {

	/**
	 * 해당 배치에 속한 Job 목록 (집계용: success_count, failed_count, stale_count, total_jobs 산출).
	 */
	List<AiJob> findByBatchExecutionId(Long batchExecutionId);

	/**
	 * 해당 배치에서 PENDING인 Job 목록. Worker가 가져갈 때 사용.
	 */
	List<AiJob> findByBatchExecutionIdAndStatus(Long batchExecutionId, AiJobStatus status);

	/**
	 * 동일 restaurant_id + job_type 에서 RUNNING 존재 여부. 동시성 제어(14번) 시 사용.
	 */
	boolean existsByRestaurantIdAndJobTypeAndStatus(Long restaurantId, AiJobType jobType, AiJobStatus status);
}
