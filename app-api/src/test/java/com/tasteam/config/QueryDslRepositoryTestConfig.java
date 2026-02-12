package com.tasteam.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.tasteam.domain.search.repository.SearchQueryProperties;

@TestConfiguration
@EnableConfigurationProperties(SearchQueryProperties.class)
@ComponentScan(basePackages = "com.tasteam.domain", useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.tasteam\\.domain\\..*\\.repository\\.impl\\..*"))
public class QueryDslRepositoryTestConfig {}
