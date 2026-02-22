package com.tasteam.domain.batch.entity;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_job")
public class AiJob extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "batch_execution_id", nullable = false)
	private BatchExecution batchExecution;

	@Enumerated(EnumType.STRING)
	@Column(name = "job_type", nullable = false, length = 50)
	private AiJobType jobType;

	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private AiJobStatus status;

	@Column(name = "base_epoch", nullable = false)
	private long baseEpoch;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	/**
	 * 배치 런에서 Job 생성 시 호출. PENDING, attempt_count=0.
	 */
	public static AiJob create(BatchExecution batchExecution, AiJobType jobType, Long restaurantId, long baseEpoch) {
		return AiJob.builder()
			.batchExecution(batchExecution)
			.jobType(jobType)
			.restaurantId(restaurantId)
			.status(AiJobStatus.PENDING)
			.baseEpoch(baseEpoch)
			.attemptCount(0)
			.build();
	}

	/**
	 * Worker가 Job 선점 시 호출. PENDING → RUNNING.
	 */
	public void markRunning() {
		this.status = AiJobStatus.RUNNING;
	}

	/**
	 * 처리 성공 시 호출.
	 */
	public void markCompleted() {
		this.status = AiJobStatus.COMPLETED;
	}

	/**
	 * 처리 실패 시 호출. attempt_count는 다음 배치에서 재시도 시 별도 증가.
	 */
	public void markFailed() {
		this.status = AiJobStatus.FAILED;
	}

	/**
	 * Epoch 재검증 실패 등으로 스킵 시 호출.
	 */
	public void markStale() {
		this.status = AiJobStatus.STALE;
	}

	/**
	 * 다음 배치에서 재시도할 때 attempt_count 1 증가.
	 */
	public void incrementAttemptCount() {
		this.attemptCount += 1;
	}
}
