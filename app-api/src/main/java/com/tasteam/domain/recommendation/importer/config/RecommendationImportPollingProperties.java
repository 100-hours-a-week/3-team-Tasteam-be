package com.tasteam.domain.recommendation.importer.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.batch.recommendation-import.polling")
public class RecommendationImportPollingProperties {

	/**
	 * 결과 파일 존재 여부 확인 주기.
	 */
	private Duration interval = Duration.ofMinutes(1);

	/**
	 * 결과 파일 대기 최대 시간.
	 */
	private Duration timeout = Duration.ofMinutes(10);

	/**
	 * 폴링 단계 최대 재시도 횟수(최초 시도 포함).
	 */
	private int pollingMaxAttempts = 3;

	/**
	 * import 단계 최대 재시도 횟수(최초 시도 포함).
	 */
	private int importMaxAttempts = 2;

	/**
	 * 재시도 간격.
	 */
	private Duration retryBackoff = Duration.ofSeconds(3);
}
