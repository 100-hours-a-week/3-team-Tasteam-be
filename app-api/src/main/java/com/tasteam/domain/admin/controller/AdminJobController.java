package com.tasteam.domain.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.batch.image.optimization.service.ImageOptimizationService;
import com.tasteam.batch.image.optimization.service.ImageOptimizationService.OptimizationResult;
import com.tasteam.domain.admin.dto.response.AdminCleanupPendingImageResponse;
import com.tasteam.domain.admin.dto.response.AdminJobResponse;
import com.tasteam.domain.admin.dto.response.AdminUnoptimizedImageResponse;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/jobs")
public class AdminJobController {

	private final ImageOptimizationService imageOptimizationService;
	private final FileService fileService;

	@GetMapping("/image-optimization/pending")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<List<AdminUnoptimizedImageResponse>> getUnoptimizedImages(
		@RequestParam(defaultValue = "100")
		int limit) {

		List<AdminUnoptimizedImageResponse> images = imageOptimizationService
			.findUnoptimizedDomainImages(limit)
			.stream()
			.map(AdminUnoptimizedImageResponse::from)
			.toList();

		return SuccessResponse.success(images);
	}

	@PostMapping("/image-optimization")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminJobResponse> runImageOptimization(
		@RequestParam(defaultValue = "100")
		int batchSize) {

		OptimizationResult result = imageOptimizationService.processOptimizationBatch(batchSize);

		return SuccessResponse.success(new AdminJobResponse(
			"image-optimization",
			result.successCount(),
			result.failedCount(),
			result.skippedCount()));
	}

	@GetMapping("/image-cleanup/pending")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<List<AdminCleanupPendingImageResponse>> getCleanupPendingImages() {
		List<AdminCleanupPendingImageResponse> images = fileService
			.findCleanupPendingImages()
			.stream()
			.map(AdminCleanupPendingImageResponse::from)
			.toList();

		return SuccessResponse.success(images);
	}

	@PostMapping("/image-cleanup")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminJobResponse> runImageCleanup() {
		int cleaned = fileService.cleanupPendingDeletedImages();

		return SuccessResponse.success(new AdminJobResponse(
			"image-cleanup",
			cleaned,
			0,
			0));
	}
}
