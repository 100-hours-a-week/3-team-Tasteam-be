package com.tasteam.domain.batch.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "batch_execution")
@Comment("배치 실행 세션 메타데이터 (시작/종료 시각, 통계)")
public class BatchExecution extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "batch_type", nullable = false, length = 50)
	private BatchType batchType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private BatchExecutionStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "total_jobs", nullable = false)
	private int totalJobs;

	@Column(name = "success_count", nullable = false)
	private int successCount;

	@Column(name = "failed_count", nullable = false)
	private int failedCount;

	@Column(name = "stale_count", nullable = false)
	private int staleCount;

	/**
	 * 배치 런 시작 시 호출. status=RUNNING, started_at=NOW(), 통계 0으로 생성.
	 */
	public static BatchExecution start(BatchType batchType, Instant startedAt) {
		return BatchExecution.builder()
			.batchType(batchType)
			.status(BatchExecutionStatus.RUNNING)
			.startedAt(startedAt)
			.finishedAt(null)
			.totalJobs(0)
			.successCount(0)
			.failedCount(0)
			.staleCount(0)
			.build();
	}

	/**
	 * 배치 종료 시 호출. finished_at·통계·status 반영. (배치 종료 시 ai_job 집계로 한 번만 반영)
	 */
	public void finish(Instant finishedAt, int totalJobs, int successCount, int failedCount, int staleCount,
		BatchExecutionStatus finalStatus) {
		this.finishedAt = finishedAt;
		this.totalJobs = totalJobs;
		this.successCount = successCount;
		this.failedCount = failedCount;
		this.staleCount = staleCount;
		this.status = finalStatus;
	}

	/**
	 * 미종료 배치를 FAILED로 표시할 때 호출.
	 */
	public void markFailed(Instant at) {
		this.finishedAt = at;
		this.status = BatchExecutionStatus.FAILED;
	}
}
