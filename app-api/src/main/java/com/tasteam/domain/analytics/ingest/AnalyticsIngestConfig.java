package com.tasteam.domain.analytics.ingest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AnalyticsIngestProperties.class)
public class AnalyticsIngestConfig {}
