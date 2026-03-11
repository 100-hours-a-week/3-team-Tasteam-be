package com.tasteam.global.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Arrays;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationLifecycleDiagnosticsLogger {

	private final Environment environment;

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		logLifecycle("application-ready");
	}

	@EventListener(ContextClosedEvent.class)
	public void onContextClosed() {
		logLifecycle("context-closed");
	}

	@EventListener
	public void onAvailabilityChange(AvailabilityChangeEvent<?> event) {
		Object state = event.getState();
		if (!(state instanceof LivenessState) && !(state instanceof ReadinessState)) {
			return;
		}
		log.info("availability changed. stateType={}, state={}, source={}",
			state.getClass().getSimpleName(),
			state,
			event.getSource().getClass().getSimpleName());
	}

	private void logLifecycle(String phase) {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		long pid = ProcessHandle.current().pid();
		long uptimeSeconds = Duration.ofMillis(runtimeMXBean.getUptime()).toSeconds();
		String hostname = resolveHostname();
		String profiles = Arrays.toString(environment.getActiveProfiles());
		log.info(
			"application lifecycle. phase={}, pid={}, hostname={}, uptimeSeconds={}, startTime={}, activeProfiles={}",
			phase,
			pid,
			hostname,
			uptimeSeconds,
			runtimeMXBean.getStartTime(),
			profiles);
	}

	private String resolveHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception ignore) {
			return "unknown";
		}
	}
}
