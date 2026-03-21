package com.tasteam.batch.image.optimization.entity;

import java.time.Instant;

import com.tasteam.domain.file.entity.Image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "image_optimization_job")
public class ImageOptimizationJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id", nullable = false, unique = true)
	private Image image;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private OptimizationJobStatus status;

	@Column(name = "original_size")
	private Long originalSize;

	@Column(name = "optimized_size")
	private Long optimizedSize;

	@Column(name = "original_width")
	private Integer originalWidth;

	@Column(name = "original_height")
	private Integer originalHeight;

	@Column(name = "optimized_width")
	private Integer optimizedWidth;

	@Column(name = "optimized_height")
	private Integer optimizedHeight;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Builder(access = AccessLevel.PRIVATE)
	private ImageOptimizationJob(Image image, OptimizationJobStatus status, Long originalSize, Long optimizedSize,
		Integer originalWidth, Integer originalHeight, Integer optimizedWidth, Integer optimizedHeight,
		String errorMessage, Instant createdAt, Instant processedAt) {
		this.image = image;
		this.status = status;
		this.originalSize = originalSize;
		this.optimizedSize = optimizedSize;
		this.originalWidth = originalWidth;
		this.originalHeight = originalHeight;
		this.optimizedWidth = optimizedWidth;
		this.optimizedHeight = optimizedHeight;
		this.errorMessage = errorMessage;
		this.createdAt = createdAt;
		this.processedAt = processedAt;
	}

	public static ImageOptimizationJob createPending(Image image) {
		return ImageOptimizationJob.builder()
			.image(image)
			.status(OptimizationJobStatus.PENDING)
			.createdAt(Instant.now())
			.build();
	}

	public void markSuccess(long originalSize, long optimizedSize, int originalWidth, int originalHeight,
		int optimizedWidth, int optimizedHeight) {
		this.status = OptimizationJobStatus.SUCCESS;
		this.originalSize = originalSize;
		this.optimizedSize = optimizedSize;
		this.originalWidth = originalWidth;
		this.originalHeight = originalHeight;
		this.optimizedWidth = optimizedWidth;
		this.optimizedHeight = optimizedHeight;
		this.processedAt = Instant.now();
	}

	public void markFailed(String errorMessage) {
		this.status = OptimizationJobStatus.FAILED;
		this.errorMessage = errorMessage;
		this.processedAt = Instant.now();
	}

	public void markSkipped(String reason) {
		this.status = OptimizationJobStatus.SKIPPED;
		this.errorMessage = reason;
		this.processedAt = Instant.now();
	}

	public void resetToPending() {
		this.status = OptimizationJobStatus.PENDING;
		this.errorMessage = null;
		this.processedAt = null;
	}

	@Override
	public String toString() {
		Long imageId = image != null ? image.getId() : null;
		return "ImageOptimizationJob{"
			+ "id=" + id
			+ ", imageId=" + imageId
			+ ", status=" + status
			+ ", originalSize=" + originalSize
			+ ", optimizedSize=" + optimizedSize
			+ ", originalWidth=" + originalWidth
			+ ", originalHeight=" + originalHeight
			+ ", optimizedWidth=" + optimizedWidth
			+ ", optimizedHeight=" + optimizedHeight
			+ ", errorMessage='" + errorMessage + '\''
			+ ", createdAt=" + createdAt
			+ ", processedAt=" + processedAt
			+ '}';
	}
}
