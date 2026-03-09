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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * {@link Transactional} 메서드의 실행 시간과 쿼리 수를 Prometheus에 수집하는 AOP 애스펙트.
 * <p>
 * {@code @Order(10)}으로 {@code @Transactional} 프록시(ORDER=MAX_INT) 외부를 감싸
 * 커밋 시간까지 포함한 실제 트랜잭션 소요 시간을 측정한다.
 * <p>
 * 수집 메트릭:
 * <ul>
 *   <li>{@code tasteam.transaction.duration} - Timer (트랜잭션 소요 시간)</li>
 *   <li>{@code tasteam.transaction.query.count} - DistributionSummary (쿼리 수 분포)</li>
 *   <li>{@code tasteam.transaction.total} - Counter (트랜잭션 수)</li>
 * </ul>
 * <p>
 * NOTE: 쿼리 카운트는 SessionFactory-wide 통계 기반으로 동시 요청 환경에서 ±N 오차 가능.
 */
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.aop.metrics", name = {"enabled",
	"transaction-metrics.enabled"}, havingValue = "true")
public class TransactionMetricsAspect {

	private static final String METRIC_DURATION = "tasteam.transaction.duration";
	private static final String METRIC_QUERY_COUNT = "tasteam.transaction.query.count";
	private static final String METRIC_TOTAL = "tasteam.transaction.total";
	private static final String UNKNOWN = "unknown";

	@Nullable
	private final MeterRegistry meterRegistry;

	private final EntityManager entityManager;

	@Around("@annotation(transactional)")
	public Object measureTransaction(
		ProceedingJoinPoint joinPoint,
		Transactional transactional) throws Throwable {
		if (meterRegistry == null) {
			return joinPoint.proceed();
		}

		Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
		// NOTE: SessionFactory-wide stats. 동시 요청 환경에서 카운트 오차 가능 (±N).
		long startQueryCount = stats.getPrepareStatementCount();
		Timer.Sample sample = Timer.start(meterRegistry);
		String outcome = "success";

		try {
			Object result = joinPoint.proceed();
			return result;
		} catch (Throwable throwable) {
			outcome = "error";
			throw throwable;
		} finally {
			long queryCount = stats.getPrepareStatementCount() - startQueryCount;
			Tags tags = buildTags(joinPoint, transactional, outcome);
			recordDuration(sample, tags);
			recordQueryCount(queryCount, tags);
			recordTotal(tags);
		}
	}

	private Tags buildTags(ProceedingJoinPoint joinPoint, Transactional transactional, String outcome) {
		return Tags.of(
			Tag.of("uri", resolveRequestUri()),
			Tag.of("domain", resolveDomain(joinPoint)),
			Tag.of("method_name", joinPoint.getSignature().getName()),
			Tag.of("read_only", String.valueOf(transactional.readOnly())),
			Tag.of("outcome", outcome));
	}

	private String resolveRequestUri() {
		ServletRequestAttributes attrs = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			// 배치/비동기 컨텍스트에서는 HTTP 요청 없음
			return "none";
		}
		HttpServletRequest request = attrs.getRequest();
		Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		return pattern != null ? pattern.toString() : "unknown";
	}

	private void recordDuration(Timer.Sample sample, Tags tags) {
		MetricLabelPolicy.validate(METRIC_DURATION, toTagArray(tags));
		sample.stop(Timer.builder(METRIC_DURATION)
			.publishPercentileHistogram()
			.tags(tags)
			.register(meterRegistry));
	}

	private void recordQueryCount(long queryCount, Tags tags) {
		MetricLabelPolicy.validate(METRIC_QUERY_COUNT, toTagArray(tags));
		DistributionSummary.builder(METRIC_QUERY_COUNT)
			.publishPercentileHistogram()
			.tags(tags)
			.register(meterRegistry)
			.record(queryCount);
	}

	private void recordTotal(Tags tags) {
		MetricLabelPolicy.validate(METRIC_TOTAL, toTagArray(tags));
		meterRegistry.counter(METRIC_TOTAL, tags).increment();
	}

	private String resolveDomain(ProceedingJoinPoint joinPoint) {
		String className = joinPoint.getSignature().getDeclaringTypeName();
		// com.tasteam.domain.<domain>.service.*  → <domain>
		String[] parts = className.split("\\.");
		for (int i = 0; i < parts.length - 1; i++) {
			if ("domain".equals(parts[i])) {
				return parts[i + 1];
			}
		}
		return UNKNOWN;
	}

	private String[] toTagArray(Tags tags) {
		java.util.List<String> list = new java.util.ArrayList<>();
		for (Tag tag : tags) {
			list.add(tag.getKey());
			list.add(tag.getValue());
		}
		return list.toArray(new String[0]);
	}
}
