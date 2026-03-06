package com.tasteam.batch.dummy;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.tasteam.domain.admin.dto.response.AdminDummySeedResponse;
import com.tasteam.domain.admin.dto.response.DummySeedStatusResponse;

@Component
public class DummySeedJobTracker {

	public enum Status {
		IDLE, RUNNING, COMPLETED, FAILED, CANCELLED
	}

	private static final int TOTAL_STEPS = 7;

	private volatile Status status = Status.IDLE;
	private volatile String currentStep = null;
	private volatile int completedSteps = 0;
	private volatile String errorMessage = null;
	private volatile AdminDummySeedResponse lastResult = null;
	private volatile Instant startedAt = null;
	private volatile boolean cancelRequested = false;

	public synchronized void start() {
		this.status = Status.RUNNING;
		this.currentStep = null;
		this.completedSteps = 0;
		this.errorMessage = null;
		this.lastResult = null;
		this.startedAt = Instant.now();
		this.cancelRequested = false;
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

	public void cancel() {
		this.cancelRequested = true;
	}

	public void cancelled() {
		this.errorMessage = "사용자에 의해 시딩이 중단되었습니다";
		this.status = Status.CANCELLED;
	}

	public boolean isRunning() {
		return this.status == Status.RUNNING;
	}

	public boolean isCancelRequested() {
		return this.cancelRequested;
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
