package com.tasteam.batch.ai.review.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.batch.review-analysis")
public class ReviewAnalysisBatchProperties {

	/**
	 * 워커 스레드 풀 상한. 동시에 처리하는 Job 개수 (감정·요약 혼합).
	 */
	private int workerPoolSize = 4;

	/**
	 * 감정/요약 AI 호출 타임아웃. 미설정 시 ai.response-timeout 사용.
	 */
	private Duration responseTimeout;

	/**
	 * 배치 종료 대기 최대 시간. 이 시간 경과 시 PENDING/RUNNING 남아 있어도 강제 종료 후 finish.
	 */
	private Duration finishTimeout = Duration.ofMinutes(60);

	/**
	 * 종료 조건 확인 주기. 기본 5분.
	 */
	private Duration finishCheckInterval = Duration.ofMinutes(5);
}
