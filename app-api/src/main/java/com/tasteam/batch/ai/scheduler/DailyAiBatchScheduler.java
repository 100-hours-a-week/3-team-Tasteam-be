package com.tasteam.batch.ai.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.review.runner.ReviewAnalysisBatchRunner;
import com.tasteam.batch.ai.vector.runner.VectorUploadBatchRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 24시간 주기로 벡터 업로드 + 리뷰 분석 배치를 순서대로 시작.
 * 벡터 실행 생성·Job 디스패치 후, 리뷰 분석 RUNNING 실행을 생성해 둠. 벡터 Job 성공 시 리뷰 Job이 해당 실행에 붙음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAiBatchScheduler {

	private final VectorUploadBatchRunner vectorUploadBatchRunner;
	private final ReviewAnalysisBatchRunner reviewAnalysisBatchRunner;

	@Scheduled(cron = "${tasteam.batch.vector-upload.cron:0 0 3 * * ?}", zone = "${tasteam.batch.vector-upload.zone:Asia/Seoul}")
	public void runDailyAiBatch() {
		vectorUploadBatchRunner.startRun();
		reviewAnalysisBatchRunner.startRun();
	}
}
