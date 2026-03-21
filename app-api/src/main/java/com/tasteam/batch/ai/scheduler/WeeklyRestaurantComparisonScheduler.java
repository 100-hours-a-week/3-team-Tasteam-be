package com.tasteam.batch.ai.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.comparison.runner.RestaurantComparisonBatchRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주간 레스토랑 비교 배치 스케줄. 임시로 매일 4시 실행.
 * 원래는 매주 일요일 4시(Asia/Seoul)로 변경할 것 — cron: 0 0 4 ? * SUN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyRestaurantComparisonScheduler {

	private final RestaurantComparisonBatchRunner restaurantComparisonBatchRunner;

	@Scheduled(cron = "${tasteam.batch.restaurant-comparison.cron:0 0 4 * * ?}", zone = "${tasteam.batch.restaurant-comparison.zone:Asia/Seoul}")
	public void runWeeklyRestaurantComparisonBatch() {
		log.info("Weekly restaurant comparison batch triggered by scheduler");
		restaurantComparisonBatchRunner.startRun();
	}
}
