package com.tasteam.domain.search.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tasteam.search.query")
public class SearchQueryProperties {

	private SearchQueryStrategy strategy = SearchQueryStrategy.ONE_STEP;
	private SearchQueryStrategy fallbackStrategy = SearchQueryStrategy.ONE_STEP;
	private int candidateLimit = 200;

	public SearchQueryStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(SearchQueryStrategy strategy) {
		this.strategy = strategy;
	}

	public SearchQueryStrategy getFallbackStrategy() {
		return fallbackStrategy;
	}

	public void setFallbackStrategy(SearchQueryStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
	}

	public int getCandidateLimit() {
		return candidateLimit;
	}

	public void setCandidateLimit(int candidateLimit) {
		this.candidateLimit = candidateLimit;
	}
}
