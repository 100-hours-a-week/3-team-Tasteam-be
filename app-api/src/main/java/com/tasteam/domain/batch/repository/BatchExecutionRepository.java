package com.tasteam.domain.batch.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;

public interface BatchExecutionRepository extends JpaRepository<BatchExecution, Long> {

	/**
	 * 해당 batch_type에서 진행 중(RUNNING)인 배치 1건. 이전 배치 완료 대기 시 사용.
	 */
	Optional<BatchExecution> findByBatchTypeAndStatus(BatchType batchType, BatchExecutionStatus status);

	/**
	 * 기간별 조회 (리포트/대시보드). started_at DESC.
	 */
	List<BatchExecution> findByBatchTypeAndStartedAtBetweenOrderByStartedAtDesc(
		BatchType batchType, Instant from, Instant to, Pageable pageable);

	/**
	 * 해당 batch_type에서 finished_at이 없는(미종료) 배치 목록
	 */
	List<BatchExecution> findByBatchTypeAndFinishedAtIsNull(BatchType batchType);

	/**
	 * 해당 batch_type에서 미종료(finished_at IS NULL)인 실행을 전부 FAILED로 일괄 갱신.
	 *
	 * @return 업데이트된 행 수
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE BatchExecution e SET e.status = :failedStatus, e.finishedAt = :finishedAt WHERE e.batchType = :batchType AND e.finishedAt IS NULL")
	int markUnclosedAsFailed(
		@Param("batchType")
		BatchType batchType,
		@Param("failedStatus")
		BatchExecutionStatus failedStatus,
		@Param("finishedAt")
		Instant finishedAt);
}
