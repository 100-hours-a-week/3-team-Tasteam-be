package com.tasteam.domain.file.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
import com.tasteam.domain.file.config.FileUploadPolicyProperties;
import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.ImageSummaryRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadFileRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageDetailResponse;
import com.tasteam.domain.file.dto.response.ImageSummaryItem;
import com.tasteam.domain.file.dto.response.ImageSummaryResponse;
import com.tasteam.domain.file.dto.response.ImageUrlResponse;
import com.tasteam.domain.file.dto.response.LinkedDomainResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadItem;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
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
	private final FileUploadPolicyProperties uploadPolicyProperties;

	public PresignedUploadResponse createPresignedUploads(PresignedUploadRequest request) {
		List<PresignedUploadItem> uploads = new ArrayList<>();

		for (PresignedUploadFileRequest fileRequest : request.files()) {
			validateUploadPolicy(fileRequest);
			UUID fileUuid = UUID.randomUUID();
			String storageKey = buildStorageKey(request.purpose(), fileUuid, fileRequest.fileName(),
				fileRequest.contentType());
			Image image = Image.create(
				request.purpose(),
				fileRequest.fileName(),
				fileRequest.size(),
				fileRequest.contentType(),
				storageKey,
				fileUuid);
			imageRepository.save(image);

			long maxContentLength = Math.min(fileRequest.size(), uploadPolicyProperties.getMaxSizeBytes());
			PresignedPostResponse presignedPost = createPresignedPost(
				storageKey,
				fileRequest.contentType(),
				uploadPolicyProperties.getMinSizeBytes(),
				maxContentLength);
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

	@Transactional
	public void activateImage(String fileUuid) {
		Image image = imageRepository.findByFileUuid(parseUuid(fileUuid))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		if (image.getStatus() == ImageStatus.DELETED) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		if (image.getStatus() == ImageStatus.PENDING) {
			image.activate();
			imageRepository.save(image);
		}
	}

	@Transactional(readOnly = true)
	public Map<Long, List<DomainImageItem>> getDomainImageUrls(DomainType domainType, List<Long> domainIds) {
		if (domainIds == null || domainIds.isEmpty()) {
			return Map.of();
		}

		List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(domainType, domainIds);
		return domainImages.stream()
			.collect(Collectors.groupingBy(
				DomainImage::getDomainId,
				LinkedHashMap::new,
				Collectors.mapping(
					di -> new DomainImageItem(
						di.getImage().getId(),
						buildPublicUrl(di.getImage().getStorageKey())),
					Collectors.toList())));
	}

	@Transactional
	public void replaceDomainImage(DomainType domainType, Long domainId, String fileUuid) {
		Image image = imageRepository.findByFileUuid(parseUuid(fileUuid))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		if (image.getStatus() == ImageStatus.DELETED) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		if (image.getStatus() != ImageStatus.PENDING
			&& domainImageRepository.findByDomainTypeAndDomainIdAndImage(domainType, domainId, image).isEmpty()) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		domainImageRepository.deleteAllByDomainTypeAndDomainId(domainType, domainId);
		domainImageRepository.save(DomainImage.create(domainType, domainId, image, 0));

		if (image.getStatus() == ImageStatus.PENDING) {
			image.activate();
			imageRepository.save(image);
		}
	}

	@Transactional
	public void clearDomainImages(DomainType domainType, Long domainId) {
		domainImageRepository.deleteAllByDomainTypeAndDomainId(domainType, domainId);
	}

	@Transactional(readOnly = true)
	public String getPrimaryDomainImageUrl(DomainType domainType, Long domainId) {
		return getPrimaryDomainImageUrlMap(domainType, List.of(domainId)).get(domainId);
	}

	@Transactional(readOnly = true)
	public Map<Long, String> getPrimaryDomainImageUrlMap(DomainType domainType, List<Long> domainIds) {
		if (domainIds == null || domainIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, List<DomainImageItem>> images = getDomainImageUrls(domainType, domainIds);
		return images.entrySet().stream()
			.filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getFirst().url()));
	}

	@Transactional(readOnly = true)
	public String getPrimaryDomainImageUrlStatic(DomainType domainType, Long domainId) {
		return getPrimaryDomainImageUrlMapStatic(domainType, List.of(domainId)).get(domainId);
	}

	@Transactional(readOnly = true)
	public Map<Long, String> getPrimaryDomainImageUrlMapStatic(DomainType domainType, List<Long> domainIds) {
		if (domainIds == null || domainIds.isEmpty()) {
			return Map.of();
		}

		List<DomainImage> domainImages = domainImageRepository.findAllByDomainTypeAndDomainIdIn(domainType, domainIds);
		Map<Long, List<DomainImage>> grouped = domainImages.stream()
			.collect(Collectors.groupingBy(DomainImage::getDomainId, LinkedHashMap::new, Collectors.toList()));

		Map<Long, String> result = new LinkedHashMap<>();
		for (Map.Entry<Long, List<DomainImage>> entry : grouped.entrySet()) {
			List<DomainImage> images = entry.getValue();
			if (images == null || images.isEmpty()) {
				continue;
			}
			DomainImage first = images.getFirst();
			result.put(entry.getKey(), buildStaticUrl(first.getImage().getStorageKey()));
		}
		return result;
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

	@Transactional(readOnly = true)
	public List<Image> findCleanupPendingImages() {
		Instant deletionCutoff = Instant.now().minus(cleanupProperties.ttlDuration());

		List<Image> pendingExpired = imageRepository.findAllByStatusAndDeletedAtIsNullAndCreatedAtBefore(
			ImageStatus.PENDING,
			deletionCutoff);

		List<Image> markedAndExpired = imageRepository.findAllByStatusAndDeletedAtBefore(
			ImageStatus.PENDING,
			deletionCutoff);

		List<Image> result = new java.util.ArrayList<>(pendingExpired);
		result.addAll(markedAndExpired);
		return result;
	}

	@Transactional
	public int cleanupPendingDeletedImages() {
		Instant deletionCutoff = Instant.now().minus(cleanupProperties.ttlDuration());

		markExpiredPendingImages(deletionCutoff);

		List<Image> targets = imageRepository.findAllByStatusAndDeletedAtBefore(
			ImageStatus.PENDING,
			deletionCutoff);
		int cleaned = 0;

		for (Image image : targets) {
			deleteObject(image.getStorageKey());
			image.cleanup();
			cleaned++;
		}

		return cleaned;
	}

	private void markExpiredPendingImages(Instant cutoff) {
		List<Image> expired = imageRepository.findAllByStatusAndDeletedAtIsNullAndCreatedAtBefore(
			ImageStatus.PENDING,
			cutoff);
		for (Image image : expired) {
			image.markDeletedAt(Instant.now());
		}
	}

	private PresignedPostResponse createPresignedPost(String storageKey, String contentType, long minContentLength,
		long maxContentLength) {
		Assert.isTrue(minContentLength > 0, "업로드 파일 최소 크기는 1바이트 이상이어야 합니다");
		Assert.isTrue(maxContentLength >= minContentLength, "업로드 파일 최대 크기는 최소 크기 이상이어야 합니다");
		try {
			return storageClient.createPresignedPost(
				new PresignedPostRequest(storageKey, contentType, minContentLength, maxContentLength));
		} catch (BusinessException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new BusinessException(FileErrorCode.STORAGE_ERROR, ex.getMessage());
		}
	}

	private void validateUploadPolicy(PresignedUploadFileRequest fileRequest) {
		long size = fileRequest.size();
		if (size < uploadPolicyProperties.getMinSizeBytes()
			|| size > uploadPolicyProperties.getMaxSizeBytes()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		if (!uploadPolicyProperties.isAllowedContentType(fileRequest.contentType())) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private void deleteObject(String storageKey) {
		try {
			storageClient.deleteObject(storageKey);
		} catch (BusinessException ex) {
			throw ex;
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

	private String buildStorageKey(FilePurpose purpose, UUID fileUuid, String fileName, String contentType) {
		String prefix = buildPrefixByPurpose(purpose);
		String extension = resolveExtension(fileName, contentType);
		if (extension.isBlank()) {
			return prefix + "/" + fileUuid;
		}
		return prefix + "/" + fileUuid + "." + extension;
	}

	private String buildPrefixByPurpose(FilePurpose purpose) {
		if (purpose == null) {
			return normalizePrefix(storageProperties.getTempUploadPrefix());
		}
		return switch (purpose) {
			case REVIEW_IMAGE -> "uploads/review/image";
			case RESTAURANT_IMAGE -> "uploads/restaurant/image";
			case MENU_IMAGE -> "uploads/menu/image";
			case PROFILE_IMAGE -> "uploads/profile/image";
			case GROUP_IMAGE -> "uploads/group/image";
			case CHAT_IMAGE -> "uploads/chat/image";
			case COMMON_ASSET -> "uploads/common/asset";
		};
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
		if (storageProperties.isPresignedAccess()) {
			return storageClient.createPresignedGetUrl(storageKey);
		}
		return buildStaticUrl(storageKey);
	}

	public String getPublicUrl(String storageKey) {
		return buildPublicUrl(storageKey);
	}

	@Transactional(readOnly = true)
	public String getImageStaticUrl(String fileUuid) {
		Image image = imageRepository.findByFileUuid(parseUuid(fileUuid))
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

		if (image.getStatus() != ImageStatus.ACTIVE) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}

		return buildStaticUrl(image.getStorageKey());
	}

	private String buildStaticUrl(String storageKey) {
		String baseUrl = storageProperties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = String.format("https://%s.s3.%s.amazonaws.com",
				storageProperties.getBucket(),
				storageProperties.getRegion());
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
