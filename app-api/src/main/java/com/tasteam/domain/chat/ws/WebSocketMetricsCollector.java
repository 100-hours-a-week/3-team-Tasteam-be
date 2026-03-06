package com.tasteam.domain.chat.ws;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketMetricsCollector {

	private final AtomicInteger activeConnections = new AtomicInteger();
	@Nullable
	private final MeterRegistry meterRegistry;

	@PostConstruct
	public void init() {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.gauge("ws.connections.active", Tags.empty(), activeConnections);
	}

	public void recordConnect() {
		if (meterRegistry == null) {
			return;
		}
		activeConnections.incrementAndGet();
		MetricLabelPolicy.validate("ws.connect.total");
		meterRegistry.counter("ws.connect.total").increment();
	}

	public void recordDisconnect() {
		if (meterRegistry == null) {
			return;
		}
		activeConnections.updateAndGet(value -> Math.max(0, value - 1));
		MetricLabelPolicy.validate("ws.disconnect.total");
		meterRegistry.counter("ws.disconnect.total").increment();
	}

	public int currentActiveConnections() {
		return activeConnections.get();
	}

	public void setActiveConnections(int value) {
		activeConnections.set(Math.max(0, value));
	}

	public void recordReconnect() {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("ws.reconnect.total");
		meterRegistry.counter("ws.reconnect.total").increment();
	}

	public void recordDisconnectByReason(String reason) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("ws.disconnect.by.reason.total", "reason", reason);
		meterRegistry.counter("ws.disconnect.by.reason.total", "reason", reason).increment();
	}

	public void recordHeartbeatTimeout() {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("ws.heartbeat.timeout.total");
		meterRegistry.counter("ws.heartbeat.timeout.total").increment();
	}

	public void recordSessionLifetime(Duration lifetime) {
		if (meterRegistry == null) {
			return;
		}
		Timer.builder("ws.session.lifetime")
			.publishPercentileHistogram()
			.register(meterRegistry)
			.record(lifetime);
	}
}
