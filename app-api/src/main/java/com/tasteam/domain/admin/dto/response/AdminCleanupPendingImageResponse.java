package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.tasteam.domain.file.entity.Image;

public record AdminCleanupPendingImageResponse(
	Long imageId,
	UUID fileUuid,
	String fileName,
	long fileSize,
	String fileType,
	String purpose,
	String status,
	Instant createdAt,
	Instant deletedAt) {

	public static AdminCleanupPendingImageResponse from(Image image) {
		return new AdminCleanupPendingImageResponse(
			image.getId(),
			image.getFileUuid(),
			image.getFileName(),
			image.getFileSize(),
			image.getFileType(),
			image.getPurpose().name(),
			image.getStatus().name(),
			image.getCreatedAt(),
			image.getDeletedAt());
	}
}
