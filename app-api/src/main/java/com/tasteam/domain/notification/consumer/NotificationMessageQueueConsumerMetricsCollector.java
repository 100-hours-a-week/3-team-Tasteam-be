package com.tasteam.domain.notification.consumer;

import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationMessageQueueConsumerMetricsCollector {

	@Nullable
	private final MeterRegistry meterRegistry;

	public void recordProcessResult(String result) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("notification.consumer.process", "result", result);
		meterRegistry.counter("notification.consumer.process", "result", result).increment();
	}

	public void recordProcessLatency(String result, long elapsedNanos) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("notification.consumer.process.latency", "result", result);
		Timer.builder("notification.consumer.process.latency")
			.publishPercentileHistogram()
			.tag("result", result)
			.register(meterRegistry)
			.record(elapsedNanos, TimeUnit.NANOSECONDS);
	}

	public void recordDlqResult(String result) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("notification.consumer.dlq", "result", result);
		meterRegistry.counter("notification.consumer.dlq", "result", result).increment();
	}
}
