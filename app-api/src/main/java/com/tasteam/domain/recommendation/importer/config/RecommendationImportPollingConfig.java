package com.tasteam.domain.recommendation.importer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.tasteam.batch.recommendation.config.RecommendationImportSchedulerProperties;

@Configuration
@EnableConfigurationProperties({
	RecommendationImportPollingProperties.class,
	RecommendationImportSchedulerProperties.class
})
public class RecommendationImportPollingConfig {}
