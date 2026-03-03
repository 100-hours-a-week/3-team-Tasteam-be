package com.tasteam.domain.file.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.file.controller.docs.FileControllerDocs;
import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.ImageSummaryRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageDetailResponse;
import com.tasteam.domain.file.dto.response.ImageSummaryResponse;
import com.tasteam.domain.file.dto.response.ImageUrlResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.infra.storage.StorageProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/files")
@Validated
@RequiredArgsConstructor
public class FileController implements FileControllerDocs {

	private final FileService fileService;
	private final StorageProperties storageProperties;

	@PostMapping("/uploads/presigned")
	public SuccessResponse<PresignedUploadResponse> createPresignedUploads(
		@RequestBody @Validated
		PresignedUploadRequest request) {
		return SuccessResponse.success(fileService.createPresignedUploads(request));
	}

	@PostMapping("/domain-links")
	public SuccessResponse<DomainImageLinkResponse> linkDomainImage(
		@RequestBody @Validated
		DomainImageLinkRequest request) {
		return SuccessResponse.success(fileService.linkDomainImage(request));
	}

	@GetMapping("/{fileUuid}")
	public SuccessResponse<ImageDetailResponse> getImageDetail(
		@PathVariable
		String fileUuid) {
		return SuccessResponse.success(fileService.getImageDetail(fileUuid));
	}

	@GetMapping("/{fileUuid}/url")
	public SuccessResponse<ImageUrlResponse> getImageUrl(
		@PathVariable
		String fileUuid,
		HttpServletResponse response) {
		if (storageProperties.isPresignedAccess()) {
			long maxAge = Math.max(storageProperties.getPresignedExpirationSeconds() - 60, 0);
			response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + maxAge);
		}
		return SuccessResponse.success(fileService.getImageUrl(fileUuid));
	}

	@PostMapping("/summary")
	public SuccessResponse<ImageSummaryResponse> getImageSummary(
		@RequestBody @Validated
		ImageSummaryRequest request) {
		return SuccessResponse.success(fileService.getImageSummary(request));
	}
}
