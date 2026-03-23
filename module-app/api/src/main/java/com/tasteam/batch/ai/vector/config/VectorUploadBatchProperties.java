package com.tasteam.batch.ai.vector.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.batch.vector-upload")
public class VectorUploadBatchProperties {

	/**
	 * 워커 스레드 풀 상한. 동시에 처리하는 Job 개수.
	 */
	private int workerPoolSize = 4;

	/**
	 * 벡터 업로드 AI 호출 타임아웃. 미설정 시 ai.response-timeout 사용.
	 */
	private Duration responseTimeout;

	/**
	 * 배치 종료 대기 최대 시간. 이 시간 경과 시 남은 PENDING/RUNNING을 FAILED 처리 후 종료.
	 */
	private Duration finishTimeout = Duration.ofMinutes(10);

	/**
	 * RUNNING 실행에 대한 종료 조건 확인 주기.
	 */
	private Duration finishCheckInterval = Duration.ofMinutes(1);
}
