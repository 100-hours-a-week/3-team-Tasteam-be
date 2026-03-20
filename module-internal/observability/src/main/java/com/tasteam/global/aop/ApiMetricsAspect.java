package com.tasteam.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 컨트롤러 메서드의 API 요청 지표를 Prometheus에 수집하는 AOP 애스펙트.
 * <p>
 * 수집 메트릭:
 * <ul>
 *   <li>{@code tasteam.api.request.duration} - Timer (응답 시간)</li>
 *   <li>{@code tasteam.api.request.total} - Counter (요청 수)</li>
 * </ul>
 * traceId는 Prometheus 라벨이 아닌 Exemplar로 주입하여 카디널리티 폭발을 방지한다.
 */
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.aop.metrics", name = {"enabled",
	"api-metrics.enabled"}, havingValue = "true")
public class ApiMetricsAspect {

	private static final String METRIC_DURATION = "tasteam.api.request.duration";
	private static final String METRIC_TOTAL = "tasteam.api.request.total";
	private static final String UNKNOWN = "unknown";

	@Nullable
	private final MeterRegistry meterRegistry;

	@Around("execution(* com.tasteam.domain.*.controller.*.*(..))")
	public Object measureApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
		if (meterRegistry == null) {
			return joinPoint.proceed();
		}

		Timer.Sample sample = Timer.start(meterRegistry);
		String outcome = "success";

		try {
			Object result = joinPoint.proceed();
			outcome = resolveHttpOutcome();
			return result;
		} catch (Throwable throwable) {
			outcome = "server_error";
			throw throwable;
		} finally {
			Tags tags = buildTags(joinPoint, outcome);
			recordDuration(sample, tags);
			recordTotal(tags);
		}
	}

	private Tags buildTags(ProceedingJoinPoint joinPoint, String outcome) {
		return Tags.of(
			Tag.of("method", resolveHttpMethod()),
			Tag.of("uri", resolveUriTemplate()),
			Tag.of("domain", resolveDomain(joinPoint)),
			Tag.of("outcome", outcome));
	}

	private void recordDuration(Timer.Sample sample, Tags tags) {
		String[] tagArray = toTagArray(tags);
		MetricLabelPolicy.validate(METRIC_DURATION, tagArray);
		sample.stop(Timer.builder(METRIC_DURATION)
			.publishPercentileHistogram()
			.tags(tags)
			.register(meterRegistry));
	}

	private void recordTotal(Tags tags) {
		String[] tagArray = toTagArray(tags);
		MetricLabelPolicy.validate(METRIC_TOTAL, tagArray);
		meterRegistry.counter(METRIC_TOTAL, tags).increment();
	}

	private String resolveHttpMethod() {
		ServletRequestAttributes attrs = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return UNKNOWN;
		}
		return attrs.getRequest().getMethod();
	}

	private String resolveUriTemplate() {
		ServletRequestAttributes attrs = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return UNKNOWN;
		}
		HttpServletRequest request = attrs.getRequest();
		Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (pattern == null) {
			return UNKNOWN;
		}
		return pattern.toString();
	}

	private String resolveDomain(ProceedingJoinPoint joinPoint) {
		String className = joinPoint.getSignature().getDeclaringTypeName();
		// com.tasteam.domain.<domain>.controller.*  → <domain>
		String[] parts = className.split("\\.");
		for (int i = 0; i < parts.length - 1; i++) {
			if ("domain".equals(parts[i])) {
				return parts[i + 1];
			}
		}
		return UNKNOWN;
	}

	private String resolveHttpOutcome() {
		ServletRequestAttributes attrs = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return "success";
		}
		HttpServletResponse response = attrs.getResponse();
		if (response == null) {
			return "success";
		}
		int status = response.getStatus();
		if (status >= 500) {
			return "server_error";
		}
		if (status >= 400) {
			return "client_error";
		}
		return "success";
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
