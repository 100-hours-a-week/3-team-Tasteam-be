package com.tasteam.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@Order(11)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.aop.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DbQueryCountAspect {

	private static final String METRIC_QUERY_COUNT = "db.query.count";

	@Nullable
	private final MeterRegistry meterRegistry;

	private final EntityManager entityManager;

	@Around("@annotation(observedDbQueryCount)")
	public Object measureDbQueryCount(
		ProceedingJoinPoint joinPoint,
		ObservedDbQueryCount observedDbQueryCount) throws Throwable {
		if (meterRegistry == null) {
			return joinPoint.proceed();
		}

		Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
		if (!stats.isStatisticsEnabled()) {
			stats.setStatisticsEnabled(true);
		}
		long startQueryCount = stats.getPrepareStatementCount();
		try {
			return joinPoint.proceed();
		} finally {
			long queryCount = Math.max(0L, stats.getPrepareStatementCount() - startQueryCount);
			if (queryCount > 0L) {
				String api = observedDbQueryCount.api();
				MetricLabelPolicy.validate(METRIC_QUERY_COUNT, "api", api);
				meterRegistry.counter(METRIC_QUERY_COUNT, "api", api).increment(queryCount);
			}
		}
	}
}
