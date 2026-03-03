package com.tasteam.batch.ai.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.batch.ai.comparison.config.RestaurantComparisonBatchProperties;
import com.tasteam.batch.ai.event.BatchExecutionFinishedEvent;
import com.tasteam.batch.ai.review.config.ReviewAnalysisBatchProperties;
import com.tasteam.batch.ai.vector.config.VectorUploadBatchProperties;
import com.tasteam.domain.batch.dto.JobStatusCount;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 배치 실행 종료: Job 집계 후 BatchExecution.finish.
 * PENDING/RUNNING 남으면 finishTimeout 경과 시 FAILED 처리 후 종료. 배치 타입별 timeout 설정 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBatchFinishService {

	private final BatchExecutionRepository batchExecutionRepository;
	private final AiJobRepository aiJobRepository;
	private final VectorUploadBatchProperties vectorUploadProperties;
	private final ReviewAnalysisBatchProperties reviewAnalysisProperties;
	private final RestaurantComparisonBatchProperties restaurantComparisonProperties;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public boolean tryFinish(long batchExecutionId, Instant now) {
		Optional<BatchExecution> executionOpt = batchExecutionRepository.findById(batchExecutionId);
		if (executionOpt.isEmpty()) {
			return false;
		}
		BatchExecution execution = executionOpt.get();
		if (execution.getStatus() != BatchExecutionStatus.RUNNING) {
			return false;
		}

		BatchType batchType = execution.getBatchType();
		Duration finishTimeout = getFinishTimeout(batchType);

		Counts counts = Counts.from(aiJobRepository.countByBatchExecutionIdGroupByStatus(batchExecutionId));

		if (counts.pending() + counts.running() > 0) {
			Duration elapsed = Duration.between(execution.getStartedAt(), now);
			if (elapsed.compareTo(finishTimeout) < 0) {
				return false;
			}
			log.warn("Batch timeout ({}): batchExecutionId={}, marking {} PENDING + {} RUNNING as FAILED",
				batchType, batchExecutionId, counts.pending(), counts.running());
			int marked = aiJobRepository.markPendingAndRunningAsFailedByBatchExecutionId(
				batchExecutionId, AiJobStatus.FAILED, AiJobStatus.PENDING, AiJobStatus.RUNNING);
			finishAndLog(execution, batchExecutionId, now, counts, counts.failed() + marked,
				BatchExecutionStatus.TIMEOUT);
			return true;
		}

		BatchExecutionStatus finalStatus = counts.failed() > 0 || counts.stale() > 0
			? BatchExecutionStatus.FAILED
			: BatchExecutionStatus.COMPLETED;
		finishAndLog(execution, batchExecutionId, now, counts, counts.failed(), finalStatus);
		return true;
	}

	private Duration getFinishTimeout(BatchType batchType) {
		return switch (batchType) {
			case VECTOR_UPLOAD_DAILY -> vectorUploadProperties.getFinishTimeout();
			case REVIEW_ANALYSIS_DAILY -> reviewAnalysisProperties.getFinishTimeout();
			case RESTAURANT_COMPARISON_WEEKLY -> restaurantComparisonProperties.getFinishTimeout();
		};
	}

	private record Counts(long completed, long failed, long stale, long pending, long running) {
		static Counts from(List<JobStatusCount> list) {
			var c = list.stream().collect(Collectors.toMap(JobStatusCount::status, JobStatusCount::count));
			return new Counts(
				c.getOrDefault(AiJobStatus.COMPLETED, 0L),
				c.getOrDefault(AiJobStatus.FAILED, 0L),
				c.getOrDefault(AiJobStatus.STALE, 0L),
				c.getOrDefault(AiJobStatus.PENDING, 0L),
				c.getOrDefault(AiJobStatus.RUNNING, 0L));
		}

		long total() {
			return completed + failed + stale + pending + running;
		}
	}

	private void finishAndLog(BatchExecution execution, long batchExecutionId, Instant now,
		Counts counts, long failed, BatchExecutionStatus finalStatus) {
		execution.finish(now, (int)counts.total(), (int)counts.completed(), (int)failed, (int)counts.stale(),
			finalStatus);
		batchExecutionRepository.save(execution);
		log.info("Batch finished ({}): batchExecutionId={}, status={}, total={}, success={}, failed={}, stale={}",
			execution.getBatchType(), batchExecutionId, finalStatus, counts.total(), counts.completed(), failed,
			counts.stale());
		eventPublisher.publishEvent(
			new BatchExecutionFinishedEvent(
				execution.getId(),
				execution.getBatchType(),
				execution.getStatus(),
				execution.getStartedAt(),
				execution.getFinishedAt(),
				execution.getTotalJobs(),
				execution.getSuccessCount(),
				execution.getFailedCount(),
				execution.getStaleCount()));
	}
}
