package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.Image;

public record AdminUnoptimizedImageResponse(
	Long domainImageId,
	Long imageId,
	UUID fileUuid,
	String fileName,
	long fileSize,
	String fileType,
	String purpose,
	String domainType,
	Long domainId,
	Instant createdAt) {
	public static AdminUnoptimizedImageResponse from(DomainImage domainImage) {
		Image image = domainImage.getImage();
		return new AdminUnoptimizedImageResponse(
			domainImage.getId(),
			image.getId(),
			image.getFileUuid(),
			image.getFileName(),
			image.getFileSize(),
			image.getFileType(),
			image.getPurpose().name(),
			domainImage.getDomainType().name(),
			domainImage.getDomainId(),
			image.getCreatedAt());
	}
}
