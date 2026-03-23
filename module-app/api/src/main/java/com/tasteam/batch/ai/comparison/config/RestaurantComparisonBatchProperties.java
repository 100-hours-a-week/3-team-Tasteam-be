package com.tasteam.batch.ai.comparison.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.batch.restaurant-comparison")
public class RestaurantComparisonBatchProperties {

	/**
	 * 워커 스레드 풀 상한. 동시에 처리하는 비교 분석 Job 개수.
	 */
	private int workerPoolSize = 4;

	/**
	 * 배치 종료 대기 최대 시간. 이 시간 경과 시 PENDING/RUNNING 남아 있어도 강제 종료 후 finish.
	 */
	private Duration finishTimeout = Duration.ofMinutes(10);

	/**
	 * 종료 조건 확인 주기.
	 */
	private Duration finishCheckInterval = Duration.ofMinutes(1);
}
