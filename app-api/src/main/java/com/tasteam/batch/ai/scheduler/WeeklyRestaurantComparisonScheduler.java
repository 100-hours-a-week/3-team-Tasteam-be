package com.tasteam.batch.ai.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.comparison.runner.RestaurantComparisonBatchRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매주 일요일 새벽 5시(Asia/Seoul)에 주간 레스토랑 비교 분석 배치를 한 번만 시작.
 * 일일 벡터/리뷰 배치와 독립 실행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyRestaurantComparisonScheduler {

	private final RestaurantComparisonBatchRunner restaurantComparisonBatchRunner;

	@Scheduled(cron = "${tasteam.batch.restaurant-comparison.cron:0 0 5 ? * SUN}", zone = "${tasteam.batch.restaurant-comparison.zone:Asia/Seoul}")
	public void runWeeklyRestaurantComparisonBatch() {
		log.info("Weekly restaurant comparison batch triggered by scheduler");
		restaurantComparisonBatchRunner.startRun();
	}
}
