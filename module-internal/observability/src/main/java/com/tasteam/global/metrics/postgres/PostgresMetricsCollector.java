package com.tasteam.global.metrics.postgres;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Session;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

@Component
public class PostgresMetricsCollector {

	private static final String POSTGRES_CACHE_HIT_RATIO_SQL = """
		select case
			when (sum(blks_hit) + sum(blks_read)) = 0 then 1.0
			else sum(blks_hit)::double precision / (sum(blks_hit) + sum(blks_read))
		end
		from pg_stat_database
		where datname = current_database()
		""";

	private final EntityManager entityManager;
	private final JdbcTemplate jdbcTemplate;
	private final long slowQueryThresholdMillis;

	private final Map<String, QuerySnapshot> previousSnapshot = new ConcurrentHashMap<>();
	private final AtomicReference<Double> postgresCacheHitRatio = new AtomicReference<>(1.0d);

	@Nullable
	private final Timer queryDurationTimer;
	@Nullable
	private final Counter slowQueriesCounter;

	public PostgresMetricsCollector(
		EntityManager entityManager,
		JdbcTemplate jdbcTemplate,
		@Nullable
		MeterRegistry meterRegistry,
		@Value("${tasteam.metrics.postgres.slow-query-threshold-ms:500}")
		long slowQueryThresholdMillis) {
		this.entityManager = entityManager;
		this.jdbcTemplate = jdbcTemplate;
		this.slowQueryThresholdMillis = slowQueryThresholdMillis;

		if (meterRegistry == null) {
			this.queryDurationTimer = null;
			this.slowQueriesCounter = null;
			return;
		}

		this.queryDurationTimer = Timer.builder("postgres_query_duration_seconds")
			.publishPercentileHistogram()
			.register(meterRegistry);
		this.slowQueriesCounter = meterRegistry.counter("postgres_slow_queries_total");
		Gauge.builder("postgres_cache_hit_ratio", postgresCacheHitRatio, AtomicReference::get)
			.register(meterRegistry);
	}

	@PostConstruct
	void initialize() {
		if (queryDurationTimer == null) {
			return;
		}
		snapshotCurrentQueryStats();
		refreshCacheHitRatio();
	}

	@Scheduled(fixedDelayString = "${tasteam.metrics.postgres.refresh-delay:30000}")
	public void collect() {
		if (queryDurationTimer == null) {
			return;
		}

		collectQueryDurations();
		refreshCacheHitRatio();
	}

	private void collectQueryDurations() {
		Statistics statistics = resolveStatistics();
		Map<String, QuerySnapshot> current = readQuerySnapshot(statistics);
		long slowQueryDelta = 0L;

		for (Map.Entry<String, QuerySnapshot> entry : current.entrySet()) {
			String query = entry.getKey();
			QuerySnapshot currentSnapshot = entry.getValue();
			QuerySnapshot previous = previousSnapshot.get(query);
			if (previous == null) {
				continue;
			}

			long countDelta = currentSnapshot.executionCount - previous.executionCount;
			long totalTimeDeltaMillis = currentSnapshot.executionTotalTimeMillis - previous.executionTotalTimeMillis;
			if (countDelta <= 0 || totalTimeDeltaMillis < 0) {
				continue;
			}

			double avgMillis = (double)totalTimeDeltaMillis / (double)countDelta;
			Duration avgDuration = Duration.ofNanos(Math.max(1L, Math.round(avgMillis * 1_000_000d)));
			for (long i = 0; i < countDelta; i++) {
				queryDurationTimer.record(avgDuration);
			}

			if (avgMillis >= slowQueryThresholdMillis) {
				slowQueryDelta += countDelta;
			}
		}

		previousSnapshot.clear();
		previousSnapshot.putAll(current);
		if (slowQueryDelta > 0 && slowQueriesCounter != null) {
			slowQueriesCounter.increment(slowQueryDelta);
		}
	}

	private void snapshotCurrentQueryStats() {
		previousSnapshot.clear();
		previousSnapshot.putAll(readQuerySnapshot(resolveStatistics()));
	}

	private Map<String, QuerySnapshot> readQuerySnapshot(Statistics statistics) {
		Map<String, QuerySnapshot> snapshot = new ConcurrentHashMap<>();
		for (String query : statistics.getQueries()) {
			if (query == null) {
				continue;
			}
			QueryStatistics queryStatistics = statistics.getQueryStatistics(query);
			snapshot.put(query, new QuerySnapshot(
				queryStatistics.getExecutionCount(),
				queryStatistics.getExecutionTotalTime()));
		}
		return snapshot;
	}

	private Statistics resolveStatistics() {
		Statistics statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
		if (!statistics.isStatisticsEnabled()) {
			statistics.setStatisticsEnabled(true);
		}
		return statistics;
	}

	private void refreshCacheHitRatio() {
		try {
			Double ratio = jdbcTemplate.queryForObject(POSTGRES_CACHE_HIT_RATIO_SQL, Double.class);
			if (ratio == null || ratio.isNaN() || ratio.isInfinite()) {
				return;
			}
			postgresCacheHitRatio.set(Math.max(0d, Math.min(1d, ratio)));
		} catch (Exception ignored) {
			// pg_stat_database 접근 권한/환경 차이로 실패할 수 있으므로 무시한다.
		}
	}

	private record QuerySnapshot(
		long executionCount,
		long executionTotalTimeMillis) {
	}
}
