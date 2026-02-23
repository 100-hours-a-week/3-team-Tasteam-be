package com.tasteam.batch.ai.review.runner;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.review.service.ReviewAnalysisBatchFinishService;
import com.tasteam.batch.ai.review.service.ReviewAnalysisBatchPreConditionService;
import com.tasteam.batch.ai.review.worker.ReviewAnalysisBatchWorker;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 리뷰 분석 배치 진입점 및 종료 폴링.
 * B-3-2: startRun() → 사전 작업 → 실행 생성 → 미전달 PENDING 1회 dispatch.
 * 종료: 주기적으로 RUNNING 실행에 대해 tryFinish (타임아웃 시 강제 종료).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAnalysisBatchRunner {

	private static final BatchType BATCH_TYPE = BatchType.REVIEW_ANALYSIS_DAILY;

	private final ReviewAnalysisBatchPreConditionService preConditionService;
	private final BatchExecutionRepository batchExecutionRepository;
	private final ReviewAnalysisBatchWorker worker;
	private final ReviewAnalysisBatchFinishService finishService;

	/**
	 * 배치 런 시작. 사전 작업(B-1) → 새 RUNNING 실행 생성 → 미전달 PENDING 1회 dispatch.
	 * 스케줄 또는 벡터 배치 종료 후 연쇄에서 호출.
	 *
	 * @return 생성된 실행 (이미 RUNNING이 있으면 재사용하지 않고 새로 생성하지 않음 — 호출 전 B-1으로 정리됨)
	 */
	public BatchExecution startRun() {
		preConditionService.runPreCondition();
		Instant now = Instant.now();
		BatchExecution execution = BatchExecution.start(BATCH_TYPE, now);
		execution = batchExecutionRepository.save(execution);
		worker.processPendingJobs(execution.getId());
		log.info("Review analysis batch run started: batchExecutionId={}", execution.getId());
		return execution;
	}

	/**
	 * RUNNING인 리뷰 분석 실행이 있으면 종료 조건 확인 후 finish. 타임아웃 시 강제 종료.
	 */
	@Scheduled(fixedDelayString = "${tasteam.batch.review-analysis.finish-check-interval:PT5M}")
	public void tryFinishRunningExecution() {
		batchExecutionRepository.findByBatchTypeAndStatus(BATCH_TYPE, BatchExecutionStatus.RUNNING)
			.ifPresent(execution -> finishService.tryFinish(execution.getId(), Instant.now()));
	}
}
