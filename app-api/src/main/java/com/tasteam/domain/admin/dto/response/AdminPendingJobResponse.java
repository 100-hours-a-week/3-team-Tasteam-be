package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.tasteam.batch.image.optimization.entity.ImageOptimizationJob;
import com.tasteam.domain.file.entity.Image;

public record AdminPendingJobResponse(
	Long jobId,
	Long imageId,
	UUID fileUuid,
	String fileName,
	long fileSize,
	String fileType,
	String purpose,
	Instant jobCreatedAt) {

	public static AdminPendingJobResponse from(ImageOptimizationJob job) {
		Image image = job.getImage();
		return new AdminPendingJobResponse(
			job.getId(),
			image.getId(),
			image.getFileUuid(),
			image.getFileName(),
			image.getFileSize(),
			image.getFileType(),
			image.getPurpose().name(),
			job.getCreatedAt());
	}
}
