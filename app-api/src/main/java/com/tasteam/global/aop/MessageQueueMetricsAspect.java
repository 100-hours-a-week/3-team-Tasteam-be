package com.tasteam.global.aop;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;
import com.tasteam.infra.messagequeue.MessageQueueMessage;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MessageQueueMetricsAspect {

	@Nullable
	private final MeterRegistry meterRegistry;

	// Named Pointcuts

	@Pointcut("execution(* com.tasteam.infra.messagequeue.trace.MessageQueueTraceService.recordPublish(..))")
	private void onPublish() {}

	@Pointcut("execution(* com.tasteam.infra.messagequeue.trace.MessageQueueTraceService.recordConsumeSuccess(..))")
	private void onConsumeSuccess() {}

	@Pointcut("execution(* com.tasteam.infra.messagequeue.trace.MessageQueueTraceService.recordConsumeFail(..))")
	private void onConsumeFail() {}

	// AfterReturning — TraceService pointcut advice

	@AfterReturning("onPublish() && args(message, providerType)")
	public void afterPublish(MessageQueueMessage message, MessageQueueProviderType providerType) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("mq.publish.count", "topic", message.topic(), "provider", providerType.value(),
			"result", "success");
		meterRegistry.counter("mq.publish.count",
			"topic", message.topic(),
			"provider", providerType.value(),
			"result", "success").increment();
	}

	@AfterReturning("onConsumeSuccess() && args(message, providerType, consumerGroup, processingMillis)")
	public void afterConsumeSuccess(
		MessageQueueMessage message,
		MessageQueueProviderType providerType,
		String consumerGroup,
		long processingMillis) {
		if (meterRegistry == null) {
			return;
		}
		incrementConsumeCount(message.topic(), providerType.value(), "success");
		recordConsumeLatency(message.topic(), providerType.value(), processingMillis);
		recordEndToEndLatency(message, providerType, "success");
	}

	@AfterReturning("onConsumeFail() && args(message, providerType, consumerGroup, processingMillis, ex)")
	public void afterConsumeFail(
		MessageQueueMessage message,
		MessageQueueProviderType providerType,
		String consumerGroup,
		long processingMillis,
		Exception ex) {
		if (meterRegistry == null) {
			return;
		}
		incrementConsumeCount(message.topic(), providerType.value(), "fail");
		recordConsumeLatency(message.topic(), providerType.value(), processingMillis);
		recordEndToEndLatency(message, providerType, "fail");
	}

	// Around — @ObservedMqProcess advice

	@Around("@annotation(observedMqProcess)")
	public Object observeMqProcess(ProceedingJoinPoint pjp, ObservedMqProcess observedMqProcess) throws Throwable {
		if (meterRegistry == null) {
			return pjp.proceed();
		}

		String processMetric = observedMqProcess.metricPrefix() + ".process";
		String latencyMetric = observedMqProcess.metricPrefix() + ".process.latency";
		Timer.Sample sample = Timer.start(meterRegistry);
		String result = "success";
		try {
			return pjp.proceed();
		} catch (Throwable throwable) {
			result = "fail";
			throw throwable;
		} finally {
			MetricLabelPolicy.validate(processMetric, "result", result);
			meterRegistry.counter(processMetric, "result", result).increment();
			MetricLabelPolicy.validate(latencyMetric, "result", result);
			sample.stop(Timer.builder(latencyMetric)
				.publishPercentileHistogram()
				.tag("result", result)
				.register(meterRegistry));
		}
	}

	// Around — @ObservedMqDlq advice

	@Around("@annotation(observedMqDlq)")
	public Object observeMqDlq(ProceedingJoinPoint pjp, ObservedMqDlq observedMqDlq) throws Throwable {
		if (meterRegistry == null) {
			return pjp.proceed();
		}

		String result = "success";
		try {
			return pjp.proceed();
		} catch (Throwable throwable) {
			result = "fail";
			log.error("DLQ 발행 실패. topic={}", observedMqDlq.topic(), throwable);
			return null;
		} finally {
			MetricLabelPolicy.validate("notification.consumer.dlq", "result", result);
			meterRegistry.counter("notification.consumer.dlq", "result", result).increment();
		}
	}

	private void incrementConsumeCount(String topic, String provider, String result) {
		MetricLabelPolicy.validate("mq.consume.count", "topic", topic, "provider", provider, "result", result);
		meterRegistry.counter("mq.consume.count", "topic", topic, "provider", provider, "result", result).increment();
	}

	private void recordConsumeLatency(String topic, String provider, long processingMillis) {
		MetricLabelPolicy.validate("mq.consume.latency", "topic", topic, "provider", provider);
		Timer.builder("mq.consume.latency")
			.tag("topic", topic)
			.tag("provider", provider)
			.register(meterRegistry)
			.record(Duration.ofMillis(processingMillis));
	}

	private void recordEndToEndLatency(MessageQueueMessage message, MessageQueueProviderType providerType,
		String result) {
		MetricLabelPolicy.validate("mq.end_to_end.latency", "topic", message.topic(), "provider", providerType.value(),
			"result", result);
		long latencyMillis = Duration.between(message.occurredAt(), Instant.now()).toMillis();
		Timer.builder("mq.end_to_end.latency")
			.tag("topic", message.topic())
			.tag("provider", providerType.value())
			.tag("result", result)
			.register(meterRegistry)
			.record(Math.max(0L, latencyMillis), TimeUnit.MILLISECONDS);
	}
}
