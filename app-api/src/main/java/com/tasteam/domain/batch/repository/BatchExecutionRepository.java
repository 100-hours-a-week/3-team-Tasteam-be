package com.tasteam.domain.batch.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
