package com.tasteam.batch.ai.review.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.batch.ai.review.config.ReviewAnalysisBatchProperties;
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
 * 리뷰 분석 배치 실행 종료: Job 집계 후 BatchExecution.finish 호출.
 * PENDING/RUNNING이 남아 있으면 finishTimeout 경과 시 FAILED 처리 후 종료(무한 대기 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAnalysisBatchFinishService {

	private static final BatchType BATCH_TYPE = BatchType.REVIEW_ANALYSIS_DAILY;

	private final BatchExecutionRepository batchExecutionRepository;
	private final AiJobRepository aiJobRepository;
	private final ReviewAnalysisBatchProperties properties;

	/**
	 * 해당 실행이 종료 조건을 만족하면 집계 후 finish. 타임아웃 시 남은 PENDING/RUNNING을 FAILED로 처리 후 종료.
	 *
	 * @param batchExecutionId 실행 ID
	 * @param now              기준 시각 (보통 Instant.now())
	 * @return 종료 처리했으면 true, 아직 대기 중이면 false
	 */
	@Transactional
	public boolean tryFinish(long batchExecutionId, Instant now) {
		Optional<BatchExecution> executionOpt = batchExecutionRepository.findById(batchExecutionId);
		if (executionOpt.isEmpty()) {
			return false;
		}
		BatchExecution execution = executionOpt.get();
		if (execution.getBatchType() != BATCH_TYPE || execution.getStatus() != BatchExecutionStatus.RUNNING) {
			return false;
		}

		Counts counts = Counts.from(aiJobRepository.countByBatchExecutionIdGroupByStatus(batchExecutionId));

		if (counts.pending() + counts.running() > 0) {
			Duration elapsed = Duration.between(execution.getStartedAt(), now);
			if (elapsed.compareTo(properties.getFinishTimeout()) < 0) {
				return false;
			}
			log.warn("Review analysis batch timeout: batchExecutionId={}, marking {} PENDING + {} RUNNING as FAILED",
				batchExecutionId, counts.pending(), counts.running());
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
		log.info(
			"Review analysis batch finished: batchExecutionId={}, status={}, total={}, success={}, failed={}, stale={}",
			batchExecutionId, finalStatus, counts.total(), counts.completed(), failed, counts.stale());
	}
}
