package com.tasteam.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@Order(12)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.aop.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationProcessingMetricsAspect {

	private static final String METRIC_PROCESSING_LATENCY = "notification.processing.latency";
	private static final String NOTIFICATION_EXECUTOR = "notificationExecutor";

	@Nullable
	private final MeterRegistry meterRegistry;

	@Around("@annotation(async) && @annotation(org.springframework.context.event.EventListener)")
	public Object measureNotificationProcessingLatency(
		ProceedingJoinPoint joinPoint,
		Async async) throws Throwable {
		if (meterRegistry == null || !NOTIFICATION_EXECUTOR.equals(async.value())) {
			return joinPoint.proceed();
		}

		Timer.Sample sample = Timer.start(meterRegistry);
		String outcome = "success";
		try {
			return joinPoint.proceed();
		} catch (Throwable throwable) {
			outcome = "error";
			throw throwable;
		} finally {
			MetricLabelPolicy.validate(METRIC_PROCESSING_LATENCY, "outcome", outcome);
			sample.stop(
				Timer.builder(METRIC_PROCESSING_LATENCY)
					.publishPercentileHistogram()
					.tag("outcome", outcome)
					.register(meterRegistry));
		}
	}
}
