package com.tasteam.domain.file.controller;

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
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController implements FileControllerDocs {

	private final FileService fileService;

	@PostMapping("/uploads/presigned")
	public SuccessResponse<PresignedUploadResponse> createPresignedUploads(
		@Valid @RequestBody
		PresignedUploadRequest request) {
		return SuccessResponse.success(fileService.createPresignedUploads(request));
	}

	@PostMapping("/domain-links")
	public SuccessResponse<DomainImageLinkResponse> linkDomainImage(
		@Valid @RequestBody
		DomainImageLinkRequest request) {
		return SuccessResponse.success(fileService.linkDomainImage(request));
	}

	@GetMapping("/{fileUuid}")
	public SuccessResponse<ImageDetailResponse> getImageDetail(
		@PathVariable
		String fileUuid) {
		return SuccessResponse.success(fileService.getImageDetail(fileUuid));
	}

	@PostMapping("/summary")
	public SuccessResponse<ImageSummaryResponse> getImageSummary(
		@Valid @RequestBody
		ImageSummaryRequest request) {
		return SuccessResponse.success(fileService.getImageSummary(request));
	}
}
