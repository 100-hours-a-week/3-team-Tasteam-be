package com.tasteam.domain.file.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.tasteam.domain.file.config.FileCleanupProperties;
import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.ImageSummaryRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadFileRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageDetailResponse;
import com.tasteam.domain.file.dto.response.ImageSummaryItem;
import com.tasteam.domain.file.dto.response.ImageSummaryResponse;
import com.tasteam.domain.file.dto.response.ImageUrlResponse;
import com.tasteam.domain.file.dto.response.LinkedDomainResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadItem;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.infra.storage.PresignedPostRequest;
import com.tasteam.infra.storage.PresignedPostResponse;
import com.tasteam.infra.storage.StorageClient;
import com.tasteam.infra.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

	private final ImageRepository imageRepository;
	private final DomainImageRepository domainImageRepository;
	private final StorageClient storageClient;
	private final StorageProperties storageProperties;
	private final FileCleanupProperties cleanupProperties;

	public PresignedUploadResponse createPresignedUploads(PresignedUploadRequest request) {
		List<PresignedUploadItem> uploads = new ArrayList<>();

		for (PresignedUploadFileRequest fileRequest : request.files()) {
			UUID fileUuid = UUID.randomUUID();
			String storageKey = buildStorageKey(fileUuid, fileRequest.fileName(), fileRequest.contentType());
			Image image = Image.create(
				request.purpose(),
				fileRequest.fileName(),
				fileRequest.size(),
				fileRequest.contentType(),
				storageKey,
				fileUuid);
			imageRepository.save(image);

			PresignedPostResponse presignedPost = createPresignedPost(storageKey, fileRequest.contentType());
			uploads.add(new PresignedUploadItem(
				image.getFileUuid().toString(),
				storageKey,
				presignedPost.url(),
				presignedPost.fields(),
				presignedPost.expiresAt()));
		}

		return new PresignedUploadResponse(uploads);
	}

	@Transactional
	public DomainImageLinkResponse linkDomainImage(DomainImageLinkRequest request) {
		Image image = imageRepository.findByFileUuid(parseUuid(request.fileUuid()))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		Optional<DomainImage> existing = domainImageRepository.findByDomainTypeAndDomainIdAndImage(
			request.domainType(),
			request.domainId(),
			image);

		if (image.getStatus() == ImageStatus.DELETED) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		if (existing.isEmpty() && image.getStatus() != ImageStatus.PENDING) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		DomainImage domainImage = existing.orElseGet(() -> domainImageRepository.save(
			DomainImage.create(request.domainType(), request.domainId(), image, request.sortOrder())));

		if (request.sortOrder() != null && !request.sortOrder().equals(domainImage.getSortOrder())) {
			domainImage.changeSortOrder(request.sortOrder());
		}

		if (image.getStatus() == ImageStatus.PENDING) {
			image.activate();
		}

		return new DomainImageLinkResponse(domainImage.getId(), image.getStatus().name());
	}

	@Transactional(readOnly = true)
	public ImageDetailResponse getImageDetail(String fileUuid) {
		Image image = imageRepository.findByFileUuid(parseUuid(fileUuid))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		List<LinkedDomainResponse> linkedDomains = domainImageRepository.findAllByImage(image).stream()
			.map(domainImage -> new LinkedDomainResponse(
				domainImage.getDomainType().name(),
				domainImage.getDomainId(),
				domainImage.getSortOrder(),
				domainImage.getCreatedAt()))
			.sorted(Comparator.comparing(LinkedDomainResponse::linkedAt))
			.toList();

		return new ImageDetailResponse(
			image.getFileUuid().toString(),
			image.getStatus().name(),
			image.getPurpose().name(),
			image.getCreatedAt(),
			linkedDomains);
	}

	@Transactional(readOnly = true)
	public ImageUrlResponse getImageUrl(String fileUuid) {
		Image image = imageRepository.findByFileUuid(parseUuid(fileUuid))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		if (image.getStatus() != ImageStatus.ACTIVE) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		return new ImageUrlResponse(
			image.getFileUuid().toString(),
			buildPublicUrl(image.getStorageKey()));
	}

	@Transactional(readOnly = true)
	public ImageSummaryResponse getImageSummary(ImageSummaryRequest request) {
		List<UUID> uuids = request.fileUuids().stream()
			.map(this::parseUuid)
			.toList();

		List<Image> images = imageRepository.findAllByFileUuidIn(uuids);
		if (images.size() != uuids.size()) {
			throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
		}

		Map<UUID, Image> imageMap = images.stream()
			.collect(Collectors.toMap(Image::getFileUuid, Function.identity()));

		List<ImageSummaryItem> items = uuids.stream()
			.map(uuid -> buildSummaryItem(imageMap, uuid))
			.toList();

		return new ImageSummaryResponse(items);
	}

	@Transactional
	public int cleanupPendingDeletedImages() {
		markExpiredImages();

		List<Image> targets = imageRepository.findAllByStatusAndDeletedAtIsNotNull(ImageStatus.PENDING);
		int cleaned = 0;

		for (Image image : targets) {
			deleteObject(image.getStorageKey());
			image.cleanup();
			cleaned++;
		}

		return cleaned;
	}

	private void markExpiredImages() {
		Instant cutoff = Instant.now().minus(cleanupProperties.ttlDuration());
		List<Image> expired = imageRepository.findAllByStatusAndDeletedAtIsNullAndCreatedAtBefore(
			ImageStatus.PENDING,
			cutoff);
		for (Image image : expired) {
			image.markDeletedAt(Instant.now());
		}
	}

	private PresignedPostResponse createPresignedPost(String storageKey, String contentType) {
		try {
			return storageClient.createPresignedPost(new PresignedPostRequest(storageKey, contentType));
		} catch (RuntimeException ex) {
			throw new BusinessException(FileErrorCode.STORAGE_ERROR, ex.getMessage());
		}
	}

	private void deleteObject(String storageKey) {
		try {
			storageClient.deleteObject(storageKey);
		} catch (RuntimeException ex) {
			throw new BusinessException(FileErrorCode.STORAGE_ERROR, ex.getMessage());
		}
	}

	private UUID parseUuid(String fileUuid) {
		try {
			return UUID.fromString(fileUuid);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "fileUuid 형식이 올바르지 않습니다");
		}
	}

	private String buildStorageKey(UUID fileUuid, String fileName, String contentType) {
		String prefix = normalizePrefix(storageProperties.getTempUploadPrefix());
		String extension = resolveExtension(fileName, contentType);
		if (extension.isBlank()) {
			return prefix + "/" + fileUuid;
		}
		return prefix + "/" + fileUuid + "." + extension;
	}

	private String resolveExtension(String fileName, String contentType) {
		String extension = extractExtension(fileName);
		if (!extension.isBlank()) {
			return extension.toLowerCase(Locale.ROOT);
		}
		return switch (contentType.toLowerCase(Locale.ROOT)) {
			case "image/jpeg", "image/jpg" -> "jpg";
			case "image/png" -> "png";
			case "image/gif" -> "gif";
			case "image/webp" -> "webp";
			default -> "";
		};
	}

	private String extractExtension(String fileName) {
		if (fileName == null) {
			return "";
		}
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot < 0 || lastDot == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(lastDot + 1);
	}

	private String buildPublicUrl(String storageKey) {
		String baseUrl = storageProperties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			return storageKey;
		}
		String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		String normalizedKey = storageKey.startsWith("/") ? storageKey.substring(1) : storageKey;
		return normalizedBase + "/" + normalizedKey;
	}

	private ImageSummaryItem buildSummaryItem(Map<UUID, Image> imageMap, UUID uuid) {
		Image image = imageMap.get(uuid);
		if (image == null) {
			throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
		}
		return new ImageSummaryItem(
			image.getId(),
			image.getFileUuid().toString(),
			buildPublicUrl(image.getStorageKey()));
	}

	private String normalizePrefix(String prefix) {
		Assert.hasText(prefix, "storage.temp-upload-prefix는 필수입니다");
		return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
	}
}
