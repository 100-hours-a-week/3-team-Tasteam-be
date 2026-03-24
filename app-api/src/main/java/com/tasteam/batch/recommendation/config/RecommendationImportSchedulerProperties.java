package com.tasteam.batch.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tasteam.batch.recommendation-import")
public class RecommendationImportSchedulerProperties {

	private boolean enabled = true;
	private String cron = "0 */15 * * * ?";
	private String zone = "Asia/Seoul";
	private String s3PrefixOrUri;
	private String modelVersion;
	private String requestIdPrefix = "recommendation-import-polling";
}
