package com.tasteam.global.aop;

import java.util.Locale;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AsyncPipelineMetricsAspect {

	@Nullable
	private final MeterRegistry meterRegistry;

	@Around("@annotation(observedAsyncPipeline)")
	public Object observeAsyncPipeline(
		ProceedingJoinPoint joinPoint,
		ObservedAsyncPipeline observedAsyncPipeline) throws Throwable {
		if (meterRegistry == null) {
			return joinPoint.proceed();
		}

		String metricPrefix = "async.pipeline.%s.%s".formatted(
			sanitize(observedAsyncPipeline.domain()),
			sanitize(observedAsyncPipeline.stage()));
		Timer.Sample sample = Timer.start(meterRegistry);
		String result = "success";
		try {
			return joinPoint.proceed();
		} catch (Throwable throwable) {
			result = "fail";
			throw throwable;
		} finally {
			recordProcessMetric(metricPrefix + ".process", result);
			recordLatency(metricPrefix + ".latency", sample, result);
		}
	}

	@Around("@annotation(observedOutbox)")
	public Object observeOutboxSnapshot(
		ProceedingJoinPoint joinPoint,
		ObservedOutbox observedOutbox) throws Throwable {
		if (meterRegistry == null) {
			return joinPoint.proceed();
		}

		String metricPrefix = "outbox.%s.snapshot".formatted(sanitize(observedOutbox.name()));
		Timer.Sample sample = Timer.start(meterRegistry);
		String result = "success";
		try {
			return joinPoint.proceed();
		} catch (Throwable throwable) {
			result = "fail";
			throw throwable;
		} finally {
			recordProcessMetric(metricPrefix, result);
			recordLatency(metricPrefix + ".latency", sample, result);
		}
	}

	private void recordProcessMetric(String metricName, String result) {
		MetricLabelPolicy.validate(metricName, "result", result);
		meterRegistry.counter(metricName, "result", result).increment();
	}

	private void recordLatency(String metricName, Timer.Sample sample, String result) {
		MetricLabelPolicy.validate(metricName, "result", result);
		sample.stop(Timer.builder(metricName)
			.publishPercentileHistogram()
			.tag("result", result)
			.register(meterRegistry));
	}

	private String sanitize(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		return value.trim()
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9_\\-.]", "_");
	}
}
