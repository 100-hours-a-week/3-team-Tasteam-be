package com.tasteam.domain.notification.event;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatNotificationMetricsCollector {

	@Nullable
	private final MeterRegistry meterRegistry;

	public void recordChatNotificationCreated(int count) {
		if (meterRegistry == null || count <= 0) {
			return;
		}
		MetricLabelPolicy.validate("notification.chat.created.total");
		meterRegistry.counter("notification.chat.created.total").increment(count);
	}

	public void recordPushSkippedOnline(int count) {
		if (meterRegistry == null || count <= 0) {
			return;
		}
		MetricLabelPolicy.validate("notification.chat.push.skipped.online.total");
		meterRegistry.counter("notification.chat.push.skipped.online.total").increment(count);
	}

	public void recordPushSent(int count) {
		if (meterRegistry == null || count <= 0) {
			return;
		}
		MetricLabelPolicy.validate("notification.chat.push.sent.total");
		meterRegistry.counter("notification.chat.push.sent.total").increment(count);
	}
}
