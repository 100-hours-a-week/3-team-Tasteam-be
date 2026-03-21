package com.tasteam.domain.group.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupImageService {

	private final FileService fileService;

	public void attachLogoIfPresent(Long groupId, String logoImageFileUuid) {
		if (logoImageFileUuid == null) {
			return;
		}
		fileService.replaceDomainImage(DomainType.GROUP, groupId, logoImageFileUuid);
	}

	public String getPrimaryLogoImageUrl(Long groupId) {
		return fileService.getPrimaryDomainImageUrl(DomainType.GROUP, groupId);
	}

	public void applyLogoImagePatch(JsonNode node, Long groupId) {
		if (node == null) {
			return;
		}
		if (node.isNull()) {
			fileService.clearDomainImages(DomainType.GROUP, groupId);
			return;
		}
		if (!node.isTextual()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		fileService.replaceDomainImage(DomainType.GROUP, groupId, node.asText());
	}
}
