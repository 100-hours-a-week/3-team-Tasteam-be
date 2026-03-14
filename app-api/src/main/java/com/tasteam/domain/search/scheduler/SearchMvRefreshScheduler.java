package com.tasteam.domain.search.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tasteam.search.mv-refresh.enabled", havingValue = "true")
public class SearchMvRefreshScheduler {

	private final JdbcTemplate jdbcTemplate;

	@Scheduled(fixedDelayString = "${tasteam.search.mv-refresh.fixed-delay:PT15M}")
	public void refreshSearchMv() {
		long start = System.currentTimeMillis();
		jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY public.restaurant_search_mv");
		log.info("restaurant_search_mv refreshed in {}ms", System.currentTimeMillis() - start);
	}
}
