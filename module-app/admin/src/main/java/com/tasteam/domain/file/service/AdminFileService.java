package com.tasteam.domain.file.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.infra.storage.PresignedUrlCacheService;
import com.tasteam.infra.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminFileService {

	private final ImageRepository imageRepository;
	private final DomainImageRepository domainImageRepository;
	private final StorageProperties storageProperties;
	private final PresignedUrlCacheService presignedUrlCacheService;

	public String getPublicUrl(String storageKey) {
		if (storageProperties.isPresignedAccess()) {
			return presignedUrlCacheService.getPresignedUrl(storageKey);
		}
		return buildStaticUrl(storageKey);
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

	@Transactional(readOnly = true)
	public Map<Long, String> getPrimaryDomainImageUrlMap(DomainType domainType, List<Long> domainIds) {
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
			result.put(entry.getKey(), getPublicUrl(images.getFirst().getImage().getStorageKey()));
		}
		return result;
	}

	@Transactional
	public void linkImages(DomainType domainType, Long domainId, List<UUID> fileUuids) {
		if (fileUuids == null || fileUuids.isEmpty()) {
			return;
		}

		for (int index = 0; index < fileUuids.size(); index++) {
			UUID fileUuid = fileUuids.get(index);
			int sortOrder = index;

			Image image = imageRepository.findByFileUuid(fileUuid)
				.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

			if (image.getStatus() != ImageStatus.PENDING) {
				throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
			}

			image.activate();
			domainImageRepository.save(DomainImage.create(domainType, domainId, image, sortOrder));
		}
	}

	private UUID parseUuid(String fileUuid) {
		try {
			return UUID.fromString(fileUuid);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "fileUuid 형식이 올바르지 않습니다");
		}
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
}
