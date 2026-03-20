package com.tasteam.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;

/**
 * {@link Transactional} 메서드를 Distributed Tracing Span으로 기록하는 AOP 애스펙트.
 * <p>
 * {@code @Order(9)}으로 {@link TransactionMetricsAspect}(Order=10) 바깥을 감싸,
 * Trace 컨텍스트가 먼저 열린 상태에서 메트릭이 기록되도록 한다.
 * <p>
 * Grafana Tempo에서 폭포수(Waterfall) 다이어그램으로 각 트랜잭션의 소요 시간 확인 가능.
 * {@code TRACING_ENABLED=true} 및 {@code OTEL_ENDPOINT} 환경변수 설정 필요.
 */
@Aspect
@Component
@Order(9)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.aop.tracing", name = {"enabled",
	"transaction-tracing.enabled"}, havingValue = "true")
public class TransactionTracingAspect {

	private static final String UNKNOWN = "unknown";

	private final ObservationRegistry observationRegistry;

	@Around("@annotation(transactional)")
	public Object traceTransaction(
		ProceedingJoinPoint joinPoint,
		Transactional transactional) throws Throwable {
		String spanName = "tasteam.tx " + joinPoint.getSignature().getName();

		Observation observation = Observation.createNotStarted(spanName, observationRegistry)
			.lowCardinalityKeyValue("domain", resolveDomain(joinPoint))
			.lowCardinalityKeyValue("method_name", joinPoint.getSignature().getName())
			.lowCardinalityKeyValue("read_only", String.valueOf(transactional.readOnly()))
			.start();

		try (Observation.Scope scope = observation.openScope()) {
			return joinPoint.proceed();
		} catch (Throwable throwable) {
			observation.error(throwable);
			throw throwable;
		} finally {
			observation.stop();
		}
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
}
