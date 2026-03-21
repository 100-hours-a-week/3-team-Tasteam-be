package com.tasteam.domain.search.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

	@EventListener(ApplicationReadyEvent.class)
	public void refreshOnStartup() {
		log.info("애플리케이션 시작 시 restaurant_search_mv 초기 갱신 시작");
		refreshSearchMv();
	}

	@Scheduled(fixedDelayString = "${tasteam.search.mv-refresh.fixed-delay:PT15M}")
	public void refreshSearchMv() {
		long start = System.currentTimeMillis();
		jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY public.restaurant_search_mv");
		log.info("restaurant_search_mv refreshed in {}ms", System.currentTimeMillis() - start);
	}
}
