package com.tasteam.domain.file.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.FileErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 공통 이미지 연결 헬퍼:
 * - 파일 존재 확인
 * - PENDING 상태 확인 후 ACTIVE로 전환
 * - sortOrder는 전달된 파일 순서를 그대로 사용
 */
@Component
@RequiredArgsConstructor
public class DomainImageLinker {

	private final ImageRepository imageRepository;
	private final DomainImageRepository domainImageRepository;

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
}
