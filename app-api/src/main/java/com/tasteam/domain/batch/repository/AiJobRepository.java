package com.tasteam.domain.batch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.batch.dto.JobStatusCount;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.AiJobType;
import com.tasteam.domain.batch.entity.BatchType;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {

	/**
	 * 해당 배치에 속한 Job 목록 (집계용: success_count, failed_count, stale_count, total_jobs 산출).
	 */
	List<AiJob> findByBatchExecutionId(Long batchExecutionId);

	/**
	 * 해당 배치 실행에 속한 Job 개수.
	 */
	@Query("SELECT COUNT(j) FROM AiJob j WHERE j.batchExecution.id = :batchExecutionId")
	long countByBatchExecutionId(@Param("batchExecutionId")
	long batchExecutionId);

	/**
	 * 해당 배치 실행의 status별 Job 개수. DB에서 집계해 엔티티 로딩 없이 조회.
	 *
	 * @return status별 개수 (해당 status가 0건이면 행 없음)
	 */
	@Query("SELECT new com.tasteam.domain.batch.dto.JobStatusCount(j.status, COUNT(j)) FROM AiJob j WHERE j.batchExecution.id = :batchExecutionId GROUP BY j.status")
	List<JobStatusCount> countByBatchExecutionIdGroupByStatus(@Param("batchExecutionId")
	Long batchExecutionId);

	/**
	 * 해당 배치에서 PENDING인 Job 목록. 생성 시간 순.
	 */
	List<AiJob> findByBatchExecutionIdAndStatusOrderByCreatedAtAsc(Long batchExecutionId, AiJobStatus status);

	/**
	 * 동일 restaurant_id + job_type 에서 RUNNING 존재 여부.
	 */
	boolean existsByRestaurantIdAndJobTypeAndStatus(Long restaurantId, AiJobType jobType, AiJobStatus status);

	/**
	 * Job 선점: PENDING인 Job만 RUNNING으로 변경
	 *
	 * @return 업데이트된 행 수 (0: 다른 워커가 선점함, 1: 선점 성공)
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE AiJob j SET j.status = :runningStatus WHERE j.id = :id AND j.status = :pendingStatus")
	int claimToRunningIfPending(
		@Param("id")
		Long id,
		@Param("pendingStatus")
		AiJobStatus pendingStatus,
		@Param("runningStatus")
		AiJobStatus runningStatus);

	/**
	 * 해당 batch_type에서 미종료 실행에 속한 PENDING·RUNNING Job을 FAILED로 변경. 사전 작업에서 호출.
	 *
	 * @return 업데이트된 행 수
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE AiJob j SET j.status = :failedStatus WHERE j.batchExecution.batchType = :batchType "
		+ "AND j.batchExecution.finishedAt IS NULL AND (j.status = :pending OR j.status = :running)")
	int markUnclosedJobsAsFailed(
		@Param("batchType")
		BatchType batchType,
		@Param("pending")
		AiJobStatus pending,
		@Param("running")
		AiJobStatus running,
		@Param("failedStatus")
		AiJobStatus failedStatus);

	/**
	 * 해당 batch_type에서 RUNNING인 Job을 전부 FAILED로 변경
	 *
	 * @return 업데이트된 행 수
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE AiJob j SET j.status = :failedStatus WHERE j.status = :runningStatus AND j.batchExecution.batchType = :batchType")
	int markRunningAsFailedByBatchType(
		@Param("batchType")
		BatchType batchType,
		@Param("runningStatus")
		AiJobStatus runningStatus,
		@Param("failedStatus")
		AiJobStatus failedStatus);

	/**
	 * 해당 배치 실행에서 PENDING·RUNNING인 Job을 전부 FAILED로 변경 (타임아웃 강제 종료 시).
	 *
	 * @return 업데이트된 행 수
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE AiJob j SET j.status = :failedStatus WHERE j.batchExecution.id = :batchExecutionId AND (j.status = :pending OR j.status = :running)")
	int markPendingAndRunningAsFailedByBatchExecutionId(
		@Param("batchExecutionId")
		Long batchExecutionId,
		@Param("pending")
		AiJobStatus pending,
		@Param("running")
		AiJobStatus running,
		@Param("failedStatus")
		AiJobStatus failedStatus);

	/**
	 * 미종료 실행(finishedAt IS NULL)에 속한 RUNNING Job 목록. 보정 배치에서 COMPLETED 후보 조회용.
	 */
	@Query("SELECT j FROM AiJob j WHERE j.batchExecution.batchType = :batchType AND j.batchExecution.finishedAt IS NULL AND j.status = :status")
	List<AiJob> findByBatchTypeAndUnclosedExecutionAndStatus(
		@Param("batchType")
		BatchType batchType,
		@Param("status")
		AiJobStatus status);
}
