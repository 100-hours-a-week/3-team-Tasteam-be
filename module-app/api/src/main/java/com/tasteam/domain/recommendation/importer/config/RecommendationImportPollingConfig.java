package com.tasteam.domain.recommendation.importer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RecommendationImportPollingProperties.class)
public class RecommendationImportPollingConfig {}
