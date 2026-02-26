package com.tasteam.batch.ai.comparison.runner;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.comparison.service.RestaurantComparisonJobProducer;
import com.tasteam.batch.ai.service.AiBatchFinishService;
import com.tasteam.batch.ai.service.UnclosedAiJobStatusCorrectionService;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주간 레스토랑 비교 배치 진입점 및 종료 폴링.
 * startRun() → 사전 작업 → 실행 생성 → Job 생성·디스패치.
 * 종료: 주기적으로 RUNNING 실행에 대해 tryFinish (타임아웃 시 강제 종료).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantComparisonBatchRunner {

	private static final BatchType BATCH_TYPE = BatchType.RESTAURANT_COMPARISON_WEEKLY;

	private final UnclosedAiJobStatusCorrectionService unclosedCorrectionService;
	private final BatchExecutionRepository batchExecutionRepository;
	private final RestaurantComparisonJobProducer jobProducer;
	private final AiBatchFinishService finishService;

	/**
	 * 배치 런 시작. 사전 작업 → 새 RUNNING 실행 생성 → Job 생성·디스패치.
	 *
	 * @return 생성된 실행
	 */
	public BatchExecution startRun() {
		runPreCondition();
		Instant now = Instant.now();
		BatchExecution execution = BatchExecution.start(BATCH_TYPE, now);
		execution = batchExecutionRepository.save(execution);
		jobProducer.createAndDispatchJobs(execution);
		log.info("Restaurant comparison batch run started: batchExecutionId={}", execution.getId());
		return execution;
	}

	private void runPreCondition() {
		var result = unclosedCorrectionService.run(BATCH_TYPE);
		if (result.hasRecovered()) {
			log.info("Restaurant comparison pre-condition: corrected={}, jobsFailed={}, executionsFailed={}",
				result.correctedCount(), result.unclosedJobCount(), result.unclosedExecutionCount());
		}
	}

	/**
	 * RUNNING인 주간 비교 실행이 있으면 종료 조건 확인 후 finish. 타임아웃 시 강제 종료.
	 */
	@Scheduled(fixedDelayString = "${tasteam.batch.restaurant-comparison.finish-check-interval:PT1M}")
	public void tryFinishRunningExecution() {
		batchExecutionRepository.findByBatchTypeAndStatus(BATCH_TYPE, BatchExecutionStatus.RUNNING)
			.ifPresent(execution -> finishService.tryFinish(execution.getId(), Instant.now()));
	}
}
