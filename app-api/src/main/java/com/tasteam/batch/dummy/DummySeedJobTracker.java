package com.tasteam.batch.dummy;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.tasteam.domain.admin.dto.response.AdminDummySeedResponse;
import com.tasteam.domain.admin.dto.response.DummySeedStatusResponse;

@Component
public class DummySeedJobTracker {

	public enum Status {
		IDLE, RUNNING, COMPLETED, FAILED
	}

	private static final int TOTAL_STEPS = 7;

	private volatile Status status = Status.IDLE;
	private volatile String currentStep = null;
	private volatile int completedSteps = 0;
	private volatile String errorMessage = null;
	private volatile AdminDummySeedResponse lastResult = null;
	private volatile Instant startedAt = null;

	public synchronized void start() {
		this.status = Status.RUNNING;
		this.currentStep = null;
		this.completedSteps = 0;
		this.errorMessage = null;
		this.lastResult = null;
		this.startedAt = Instant.now();
	}

	public void updateStep(String stepName) {
		this.currentStep = stepName;
		this.completedSteps++;
	}

	public void complete(AdminDummySeedResponse result) {
		this.lastResult = result;
		this.status = Status.COMPLETED;
	}

	public void fail(String message) {
		this.errorMessage = message;
		this.status = Status.FAILED;
	}

	public boolean isRunning() {
		return this.status == Status.RUNNING;
	}

	public DummySeedStatusResponse getSnapshot() {
		Instant started = this.startedAt;
		Long elapsedMs = started != null ? Instant.now().toEpochMilli() - started.toEpochMilli() : null;
		return new DummySeedStatusResponse(
			this.status.name(),
			this.currentStep,
			this.completedSteps,
			TOTAL_STEPS,
			this.errorMessage,
			this.lastResult,
			elapsedMs);
	}
}
